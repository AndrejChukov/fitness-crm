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
import ru.fitnesscrm.memberships.api.dto.request.AssignMembershipRequest;
import ru.fitnesscrm.memberships.domain.ClientMembership;
import ru.fitnesscrm.memberships.domain.MembershipStatus;
import ru.fitnesscrm.memberships.domain.MembershipTemplate;
import ru.fitnesscrm.memberships.repository.ClientMembershipRepository;
import ru.fitnesscrm.memberships.repository.MembershipTemplateRepository;
import ru.fitnesscrm.memberships.service.ClientMembershipService;
import ru.fitnesscrm.support.AbstractIntegrationTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class FinanceServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired ClientAccountRepository clientAccountRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired ClientMembershipRepository clientMembershipRepository;
    @Autowired MembershipTemplateRepository membershipTemplateRepository;
    @Autowired ClientMembershipService clientMembershipService;
    @Autowired FinanceService financeService;
    @Autowired EntityManager entityManager;

    private Tenant tenant;
    private User client;
    private User admin;
    private MembershipTemplate membershipTemplate;
    private ClientMembership clientMembership;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setName("Gym");
        tenant.setSlug("gym-" + System.nanoTime());
        tenant.setActive(true);
        tenant = tenantRepository.save(tenant);

        client = saveUser("client-" + System.nanoTime() + "@mail.ru", Role.CLIENT);
        admin = saveUser("admin-" + System.nanoTime() + "@mail.ru", Role.TENANT_ADMIN);

        membershipTemplate = new MembershipTemplate();
        membershipTemplate.setTenantId(tenant.getId());
        membershipTemplate.setName("templ");
        membershipTemplate.setClassLimit(100);
        membershipTemplate.setPrice(new BigDecimal("1000.00"));
        membershipTemplate.setDurationDays(100);
        membershipTemplate.setActive(true);
        membershipTemplate = membershipTemplateRepository.save(membershipTemplate);

        clientMembership = new ClientMembership();
        clientMembership.setTenantId(tenant.getId());
        clientMembership.setTemplate(membershipTemplate);
        clientMembership.setStatus(MembershipStatus.ACTIVE);
        clientMembership.setClientId(client.getId());
        clientMembership.setRemainingClasses(10);
        clientMembership.setStartDate(LocalDate.now().minus(100, ChronoUnit.DAYS));
        clientMembership.setEndDate(LocalDate.now().plus(30, ChronoUnit.DAYS));
        clientMembership = clientMembershipRepository.save(clientMembership);

        TenantContext.set(tenant.getId(), admin.getId(), Role.TENANT_ADMIN);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void makeInvoice_shouldCreateUnpaidInvoiceAndZeroBalanceAccount_whenAccountMissing() {
        financeService.makeInvoice(clientMembership.getId(), client.getId(), new BigDecimal("1000.00"));

        List<Invoice> invoices = invoiceRepository.findAll();
        assertEquals(1, invoices.size());
        Invoice invoice = invoices.getFirst();
        assertEquals(0, new BigDecimal("1000.00").compareTo(invoice.getAmount()));
        assertEquals(InvoiceStatus.UNPAID, invoice.getStatus());
        assertEquals(LocalDate.now().plusDays(3), invoice.getDueDate());
        assertEquals(client.getId(), invoice.getClientId());
        assertEquals(clientMembership.getId(), invoice.getClientMembershipId());
        assertEquals(tenant.getId(), invoice.getTenantId());

        ClientAccount account = clientAccountRepository.findByClientId(client.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
        assertEquals(tenant.getId(), account.getTenantId());
    }

    @Test
    void makeInvoice_shouldNotResetExistingAccountBalance() {
        ClientAccount existing = new ClientAccount();
        existing.setTenantId(tenant.getId());
        existing.setClientId(client.getId());
        existing.setBalance(new BigDecimal("50.00"));
        clientAccountRepository.save(existing);

        financeService.makeInvoice(clientMembership.getId(), client.getId(), new BigDecimal("1000.00"));

        ClientAccount account = clientAccountRepository.findByClientId(client.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("50.00").compareTo(account.getBalance()));
        assertEquals(1, invoiceRepository.findAll().size());
    }

    @Test
    void assign_shouldCreateInvoiceAutomatically() {
        clientMembershipService.assign(new AssignMembershipRequest(client.getId(), membershipTemplate.getId()));

        entityManager.flush();
        entityManager.clear();

        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(i -> client.getId().equals(i.getClientId()))
                .toList();
        assertEquals(1, invoices.size());
        Invoice invoice = invoices.getFirst();
        assertEquals(InvoiceStatus.UNPAID, invoice.getStatus());
        assertEquals(LocalDate.now().plusDays(3), invoice.getDueDate());
        assertEquals(0, membershipTemplate.getPrice().compareTo(invoice.getAmount()));
        assertTrue(clientAccountRepository.findByClientId(client.getId()).isPresent());
    }

    @Test
    void hasNonNegativeBalance_shouldReturnTrue_whenNoAccountExists() {
        assertTrue(financeService.hasNonNegativeBalance(client.getId()));
    }

    @Test
    void hasNonNegativeBalance_shouldReturnTrue_whenBalanceIsPositive() {
        ClientAccount account = new ClientAccount();
        account.setTenantId(tenant.getId());
        account.setClientId(client.getId());
        account.setBalance(new BigDecimal("1000.00"));
        clientAccountRepository.save(account);

        assertTrue(financeService.hasNonNegativeBalance(client.getId()));
    }

    @Test
    void hasNonNegativeBalance_shouldReturnTrue_whenBalanceIsZero() {
        ClientAccount account = new ClientAccount();
        account.setTenantId(tenant.getId());
        account.setClientId(client.getId());
        account.setBalance(BigDecimal.ZERO);
        clientAccountRepository.save(account);

        assertTrue(financeService.hasNonNegativeBalance(client.getId()));
    }

    @Test
    void hasNonNegativeBalance_shouldReturnFalse_whenBalanceIsNegative() {
        ClientAccount account = new ClientAccount();
        account.setTenantId(tenant.getId());
        account.setClientId(client.getId());
        account.setBalance(new BigDecimal("-1000.00"));
        clientAccountRepository.save(account);

        assertFalse(financeService.hasNonNegativeBalance(client.getId()));
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setTenant(tenant);
        user.setEmail(email);
        user.setPasswordHash("password");
        user.setRole(role);
        user.setFirstName("Andrej");
        user.setLastName("Ivanov");
        user.setActive(true);
        return userRepository.save(user);
    }
}
