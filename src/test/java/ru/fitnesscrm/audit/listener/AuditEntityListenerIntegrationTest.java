package ru.fitnesscrm.audit.listener;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.audit.domain.AuditAction;
import ru.fitnesscrm.audit.domain.AuditLog;
import ru.fitnesscrm.audit.repository.AuditLogRepository;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.finance.domain.ClientAccount;
import ru.fitnesscrm.finance.domain.Invoice;
import ru.fitnesscrm.finance.domain.InvoiceStatus;
import ru.fitnesscrm.finance.domain.PaymentMethod;
import ru.fitnesscrm.finance.repository.ClientAccountRepository;
import ru.fitnesscrm.finance.repository.InvoiceRepository;
import ru.fitnesscrm.finance.service.FinanceService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class AuditEntityListenerIntegrationTest extends AbstractIntegrationTest {

    @Autowired InvoiceRepository invoiceRepository;
    @Autowired ClientMembershipRepository clientMembershipRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired MembershipTemplateRepository membershipTemplateRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired ClientAccountRepository clientAccountRepository;
    @Autowired FinanceService financeService;
    @Autowired EntityManager entityManager;

    private Tenant tenant;
    private User admin;
    private User client;
    private MembershipTemplate membershipTemplate;
    private ClientMembership membership;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setName("Gym");
        tenant.setSlug("gym-" + System.nanoTime());
        tenant.setActive(true);
        tenant = tenantRepository.save(tenant);

        admin = saveUser("admin-" + System.nanoTime() + "@mail.ru", Role.TENANT_ADMIN);
        client = saveUser("client-" + System.nanoTime() + "@mail.ru", Role.CLIENT);

        TenantContext.set(tenant.getId(), admin.getId(), Role.TENANT_ADMIN);

        membershipTemplate = new MembershipTemplate();
        membershipTemplate.setTenantId(tenant.getId());
        membershipTemplate.setName("templ");
        membershipTemplate.setClassLimit(100);
        membershipTemplate.setPrice(new BigDecimal("1000.00"));
        membershipTemplate.setDurationDays(100);
        membershipTemplate.setActive(true);
        membershipTemplate = membershipTemplateRepository.save(membershipTemplate);

        membership = new ClientMembership();
        membership.setTenantId(tenant.getId());
        membership.setClientId(client.getId());
        membership.setTemplate(membershipTemplate);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setRemainingClasses(8);
        membership.setFreezeDaysUsed(0);
        membership.setStartDate(LocalDate.now());
        membership.setEndDate(LocalDate.now().plusDays(30));
        membership = clientMembershipRepository.saveAndFlush(membership);

        ClientAccount account = new ClientAccount();
        account.setTenantId(tenant.getId());
        account.setClientId(client.getId());
        account.setBalance(new BigDecimal("-1000.00"));
        clientAccountRepository.save(account);

        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void invoiceStatusChange_unpaidToPaid_shouldWriteAuditLog() {
        Invoice invoice = saveUnpaidInvoice();
        entityManager.clear();

        Invoice managed = invoiceRepository.findById(invoice.getId()).orElseThrow();
        managed.setStatus(InvoiceStatus.PAID);
        invoiceRepository.saveAndFlush(managed);

        List<AuditLog> logs = auditLogsFor("Invoice", invoice.getId());
        assertEquals(1, logs.size());
        AuditLog log = logs.getFirst();
        assertEquals(AuditAction.UPDATE, log.getAction());
        assertEquals("UNPAID", log.getOldValue());
        assertEquals("PAID", log.getNewValue());
        assertEquals(tenant.getId(), log.getTenantId());
        assertEquals(admin.getId(), log.getChangedByUser());
    }

    @Test
    void payInvoice_shouldWriteAuditLog() {
        Invoice invoice = saveUnpaidInvoice();
        entityManager.clear();

        financeService.pay(invoice.getId(), PaymentMethod.CARD);
        entityManager.flush();
        entityManager.clear();

        List<AuditLog> logs = auditLogsFor("Invoice", invoice.getId());
        assertEquals(1, logs.size());
        assertEquals(AuditAction.UPDATE, logs.getFirst().getAction());
        assertEquals("UNPAID", logs.getFirst().getOldValue());
        assertEquals("PAID", logs.getFirst().getNewValue());
        assertEquals(admin.getId(), logs.getFirst().getChangedByUser());
    }

    @Test
    void invoiceUpdate_withoutStatusChange_shouldNotWriteAuditLog() {
        Invoice invoice = saveUnpaidInvoice();
        entityManager.clear();

        Invoice managed = invoiceRepository.findById(invoice.getId()).orElseThrow();
        managed.setDueDate(LocalDate.now().plusDays(10));
        invoiceRepository.saveAndFlush(managed);

        assertTrue(auditLogsFor("Invoice", invoice.getId()).isEmpty());
    }

    @Test
    void membershipStatusChange_shouldWriteAuditLog() {
        entityManager.clear();
        ClientMembership managed = clientMembershipRepository.findById(membership.getId()).orElseThrow();
        managed.setStatus(MembershipStatus.FROZEN);
        clientMembershipRepository.saveAndFlush(managed);

        List<AuditLog> logs = auditLogsFor("ClientMembership", membership.getId());
        assertEquals(1, logs.size());
        assertEquals(AuditAction.UPDATE, logs.getFirst().getAction());
        assertEquals("ACTIVE", logs.getFirst().getOldValue());
        assertEquals("FROZEN", logs.getFirst().getNewValue());
        assertEquals(admin.getId(), logs.getFirst().getChangedByUser());
    }

    @Test
    void membershipDelete_shouldWriteAuditLog() {
        Long membershipId = membership.getId();
        entityManager.clear();

        ClientMembership managed = clientMembershipRepository.findById(membershipId).orElseThrow();
        clientMembershipRepository.delete(managed);
        clientMembershipRepository.flush();

        List<AuditLog> logs = auditLogsFor("ClientMembership", membershipId);
        assertEquals(1, logs.size());
        assertEquals(AuditAction.DELETE, logs.getFirst().getAction());
        assertEquals(admin.getId(), logs.getFirst().getChangedByUser());
        assertEquals(tenant.getId(), logs.getFirst().getTenantId());
    }

    @Test
    void membershipUpdate_withoutStatusChange_shouldNotWriteAuditLog() {
        entityManager.clear();
        ClientMembership managed = clientMembershipRepository.findById(membership.getId()).orElseThrow();
        managed.setRemainingClasses(7);
        clientMembershipRepository.saveAndFlush(managed);

        assertTrue(auditLogsFor("ClientMembership", membership.getId()).isEmpty());
    }

    @Test
    void invoiceStatusChange_twiceInSamePersistenceContext_shouldWriteOneAuditPerChange() {
        Invoice invoice = saveUnpaidInvoice();
        entityManager.clear();

        Invoice managed = invoiceRepository.findById(invoice.getId()).orElseThrow();
        managed.setStatus(InvoiceStatus.OVERDUE);
        invoiceRepository.saveAndFlush(managed);

        managed.setStatus(InvoiceStatus.PAID);
        invoiceRepository.saveAndFlush(managed);

        List<AuditLog> logs = auditLogsFor("Invoice", invoice.getId()).stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .toList();
        assertEquals(2, logs.size());
        assertEquals("UNPAID", logs.get(0).getOldValue());
        assertEquals("OVERDUE", logs.get(0).getNewValue());
        assertEquals("OVERDUE", logs.get(1).getOldValue());
        assertEquals("PAID", logs.get(1).getNewValue());
    }

    private Invoice saveUnpaidInvoice() {
        Invoice invoice = new Invoice();
        invoice.setTenantId(tenant.getId());
        invoice.setClientId(client.getId());
        invoice.setClientMembershipId(membership.getId());
        invoice.setAmount(new BigDecimal("1000.00"));
        invoice.setDueDate(LocalDate.now().plusDays(3));
        invoice.setStatus(InvoiceStatus.UNPAID);
        return invoiceRepository.saveAndFlush(invoice);
    }

    private List<AuditLog> auditLogsFor(String entityName, Long entityId) {
        return auditLogRepository.findAll().stream()
                .filter(l -> entityName.equals(l.getEntityName()) && entityId.equals(l.getEntityId()))
                .toList();
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
