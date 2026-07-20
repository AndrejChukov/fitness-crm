package ru.fitnesscrm.finance.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.finance.domain.ClientAccount;
import ru.fitnesscrm.finance.domain.Invoice;
import ru.fitnesscrm.finance.domain.InvoiceStatus;
import ru.fitnesscrm.finance.repository.ClientAccountRepository;
import ru.fitnesscrm.finance.repository.InvoiceRepository;
import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.identity.domain.Tenant;
import ru.fitnesscrm.identity.domain.User;
import ru.fitnesscrm.identity.repository.TenantRepository;
import ru.fitnesscrm.identity.repository.UserRepository;
import ru.fitnesscrm.memberships.domain.ClientMembership;
import ru.fitnesscrm.memberships.domain.MembershipStatus;
import ru.fitnesscrm.memberships.domain.MembershipTemplate;
import ru.fitnesscrm.memberships.repository.ClientMembershipRepository;
import ru.fitnesscrm.memberships.repository.MembershipTemplateRepository;
import ru.fitnesscrm.support.AbstractIntegrationTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class UnpaidInvoiceJobIntegrationTest extends AbstractIntegrationTest {

    @Autowired UnpaidInvoiceJob unpaidInvoiceJob;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired ClientAccountRepository clientAccountRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired MembershipTemplateRepository membershipTemplateRepository;
    @Autowired ClientMembershipRepository clientMembershipRepository;
    @Autowired EntityManager entityManager;

    private Tenant tenant;
    private User client;
    private MembershipTemplate template;
    private ClientMembership membership;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setName("Gym");
        tenant.setSlug("gym-" + System.nanoTime());
        tenant.setActive(true);
        tenant = tenantRepository.save(tenant);

        client = new User();
        client.setTenant(tenant);
        client.setEmail("client-" + System.nanoTime() + "@mail.ru");
        client.setPasswordHash("password");
        client.setRole(Role.CLIENT);
        client.setFirstName("Andrej");
        client.setLastName("Ivanov");
        client.setActive(true);
        client = userRepository.save(client);

        template = new MembershipTemplate();
        template.setTenantId(tenant.getId());
        template.setName("templ");
        template.setClassLimit(10);
        template.setPrice(new BigDecimal("100.00"));
        template.setDurationDays(30);
        template.setActive(true);
        template = membershipTemplateRepository.save(template);

        membership = new ClientMembership();
        membership.setTenantId(tenant.getId());
        membership.setTemplate(template);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setClientId(client.getId());
        membership.setRemainingClasses(10);
        membership.setStartDate(LocalDate.now().minus(10, ChronoUnit.DAYS));
        membership.setEndDate(LocalDate.now().plus(20, ChronoUnit.DAYS));
        membership = clientMembershipRepository.save(membership);

        ClientAccount account = new ClientAccount();
        account.setTenantId(tenant.getId());
        account.setClientId(client.getId());
        account.setBalance(BigDecimal.ZERO);
        clientAccountRepository.save(account);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void processUnpaidInvoice_shouldApplyDebt_whenInvoiceIsOverdue() {
        saveInvoice(InvoiceStatus.UNPAID, LocalDate.now().minusDays(1), new BigDecimal("100.00"));

        unpaidInvoiceJob.processUnpaidInvoice();
        entityManager.flush();
        entityManager.clear();

        ClientAccount account = clientAccountRepository.findByClientId(client.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("-100.00").compareTo(account.getBalance()));
    }

    @Test
    void processUnpaidInvoice_shouldNotTouchBalance_whenDueDateIsTodayOrFuture() {
        saveInvoice(InvoiceStatus.UNPAID, LocalDate.now(), new BigDecimal("100.00"));
        saveInvoice(InvoiceStatus.UNPAID, LocalDate.now().plusDays(2), new BigDecimal("50.00"));

        unpaidInvoiceJob.processUnpaidInvoice();
        entityManager.flush();
        entityManager.clear();

        ClientAccount account = clientAccountRepository.findByClientId(client.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
    }

    @Test
    void processUnpaidInvoice_shouldIgnorePaidInvoicesEvenIfPastDue() {
        saveInvoice(InvoiceStatus.PAID, LocalDate.now().minusDays(5), new BigDecimal("100.00"));

        unpaidInvoiceJob.processUnpaidInvoice();
        entityManager.flush();
        entityManager.clear();

        ClientAccount account = clientAccountRepository.findByClientId(client.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
    }

    @Test
    void processUnpaidInvoice_shouldAccumulateDebt_forMultipleOverdueInvoices() {
        saveInvoice(InvoiceStatus.UNPAID, LocalDate.now().minusDays(1), new BigDecimal("40.00"));
        saveInvoice(InvoiceStatus.UNPAID, LocalDate.now().minusDays(2), new BigDecimal("60.00"));

        unpaidInvoiceJob.processUnpaidInvoice();
        entityManager.flush();
        entityManager.clear();

        ClientAccount account = clientAccountRepository.findByClientId(client.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("-100.00").compareTo(account.getBalance()));
    }

    private void saveInvoice(InvoiceStatus status, LocalDate dueDate, BigDecimal amount) {
        Invoice invoice = new Invoice();
        invoice.setTenantId(tenant.getId());
        invoice.setClientId(client.getId());
        invoice.setClientMembershipId(membership.getId());
        invoice.setAmount(amount);
        invoice.setDueDate(dueDate);
        invoice.setStatus(status);
        invoiceRepository.save(invoice);
    }
}
