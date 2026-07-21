package ru.fitnesscrm.finance.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.common.exception.ResourceNotFoundException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.finance.domain.ClientAccount;
import ru.fitnesscrm.finance.domain.Invoice;
import ru.fitnesscrm.finance.domain.InvoiceStatus;
import ru.fitnesscrm.finance.domain.PaymentMethod;
import ru.fitnesscrm.finance.domain.Transaction;
import ru.fitnesscrm.finance.repository.ClientAccountRepository;
import ru.fitnesscrm.finance.repository.InvoiceRepository;
import ru.fitnesscrm.finance.repository.TransactionRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class FinanceServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired ClientAccountRepository clientAccountRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired TransactionRepository transactionRepository;
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
        saveAccount(new BigDecimal("50.00"));

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
    void pay_shouldMarkPaid_clearDebt_andCreateTransaction() {
        ClientAccount account = saveAccount(new BigDecimal("-1000.00"));
        Invoice invoice = saveUnpaidInvoice(new BigDecimal("1000.00"));

        financeService.pay(invoice.getId(), PaymentMethod.CARD);

        entityManager.flush();
        entityManager.clear();

        Invoice paid = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(InvoiceStatus.PAID, paid.getStatus());
        assertNotNull(paid.getPaidAt());
        assertEquals(0, new BigDecimal("1000.00").compareTo(paid.getAmount()), "invoice amount must stay for audit");

        ClientAccount refreshed = clientAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(refreshed.getBalance()));
        assertTrue(financeService.hasNonNegativeBalance(client.getId()));

        List<Transaction> txs = transactionRepository.findAll();
        assertEquals(1, txs.size());
        Transaction tx = txs.getFirst();
        assertEquals(invoice.getId(), tx.getInvoiceId());
        assertEquals(PaymentMethod.CARD, tx.getMethod());
        assertEquals(0, new BigDecimal("1000.00").compareTo(tx.getAmount()));
        assertEquals(tenant.getId(), tx.getTenantId());
        assertNotNull(tx.getProcessedAt());
    }

    @Test
    void pay_shouldAcceptCashMethod() {
        saveAccount(new BigDecimal("-100.00"));
        Invoice invoice = saveUnpaidInvoice(new BigDecimal("100.00"));

        financeService.pay(invoice.getId(), PaymentMethod.CASH);

        entityManager.flush();
        entityManager.clear();

        assertEquals(PaymentMethod.CASH, transactionRepository.findAll().getFirst().getMethod());
        assertEquals(InvoiceStatus.PAID, invoiceRepository.findById(invoice.getId()).orElseThrow().getStatus());
    }

    @Test
    void pay_beforeDueDate_shouldIncreaseBalanceByInvoiceAmount() {
        // Current model: always balance += amount (early pay creates credit until you refine policy)
        ClientAccount account = saveAccount(BigDecimal.ZERO);
        Invoice invoice = saveUnpaidInvoice(new BigDecimal("1000.00"));
        invoice.setDueDate(LocalDate.now().plusDays(2));
        invoiceRepository.save(invoice);

        financeService.pay(invoice.getId(), PaymentMethod.CASH);

        entityManager.flush();
        entityManager.clear();

        ClientAccount refreshed = clientAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("1000.00").compareTo(refreshed.getBalance()));
        assertEquals(InvoiceStatus.PAID, invoiceRepository.findById(invoice.getId()).orElseThrow().getStatus());
    }

    @Test
    void pay_shouldRejectSecondPayment_whenInvoiceAlreadyPaid() {
        saveAccount(new BigDecimal("-1000.00"));
        Invoice invoice = saveUnpaidInvoice(new BigDecimal("1000.00"));

        financeService.pay(invoice.getId(), PaymentMethod.CASH);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> financeService.pay(invoice.getId(), PaymentMethod.CASH));
        assertEquals("Invoice is already paid", ex.getMessage());

        entityManager.flush();
        entityManager.clear();

        assertEquals(1, transactionRepository.findAll().size());
        ClientAccount account = clientAccountRepository.findByClientId(client.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
    }

    @Test
    void pay_shouldClearDebt_whenInvoiceIsOverdue() {
        ClientAccount account = saveAccount(new BigDecimal("-1000.00"));
        Invoice invoice = saveUnpaidInvoice(new BigDecimal("1000.00"));
        invoice.setStatus(InvoiceStatus.OVERDUE);
        invoiceRepository.save(invoice);

        financeService.pay(invoice.getId(), PaymentMethod.CARD);

        entityManager.flush();
        entityManager.clear();

        Invoice paid = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(InvoiceStatus.PAID, paid.getStatus());
        assertNotNull(paid.getPaidAt());

        ClientAccount refreshed = clientAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(refreshed.getBalance()));
        assertEquals(1, transactionRepository.findAll().size());
    }

    @Test
    void pay_shouldReject_whenInvoiceIsCancelled() {
        saveAccount(new BigDecimal("-100.00"));
        Invoice invoice = saveUnpaidInvoice(new BigDecimal("100.00"));
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> financeService.pay(invoice.getId(), PaymentMethod.CASH));
        assertEquals("Invoice is already paid", ex.getMessage());
        assertEquals(0, transactionRepository.findAll().size());
    }

    @Test
    void pay_shouldThrow_whenInvoiceNotFound() {
        saveAccount(new BigDecimal("1000.00"));

        assertThrows(ResourceNotFoundException.class, () -> financeService.pay(999_999L, PaymentMethod.CASH));
    }

    @Test
    void pay_shouldThrow_whenClientAccountNotFound() {
        Invoice invoice = saveUnpaidInvoice(new BigDecimal("1000.00"));

        assertThrows(ResourceNotFoundException.class, () -> financeService.pay(invoice.getId(), PaymentMethod.CASH));
    }

    @Test
    void pay_shouldThrow_whenInvoiceBelongsToAnotherTenant() {
        saveAccount(new BigDecimal("-100.00"));
        Invoice invoice = saveUnpaidInvoice(new BigDecimal("100.00"));

        Tenant otherTenant = new Tenant();
        otherTenant.setName("Other");
        otherTenant.setSlug("other-" + System.nanoTime());
        otherTenant.setActive(true);
        otherTenant = tenantRepository.save(otherTenant);

        User otherAdmin = new User();
        otherAdmin.setTenant(otherTenant);
        otherAdmin.setEmail("other-admin-" + System.nanoTime() + "@mail.ru");
        otherAdmin.setPasswordHash("password");
        otherAdmin.setRole(Role.TENANT_ADMIN);
        otherAdmin.setFirstName("Other");
        otherAdmin.setLastName("Admin");
        otherAdmin.setActive(true);
        otherAdmin = userRepository.save(otherAdmin);

        TenantContext.set(otherTenant.getId(), otherAdmin.getId(), Role.TENANT_ADMIN);

        assertThrows(ResourceNotFoundException.class,
                () -> financeService.pay(invoice.getId(), PaymentMethod.CASH));
    }

    @Test
    void hasNonNegativeBalance_shouldReturnTrue_whenNoAccountExists() {
        assertTrue(financeService.hasNonNegativeBalance(client.getId()));
    }

    @Test
    void hasNonNegativeBalance_shouldReturnTrue_whenBalanceIsPositive() {
        saveAccount(new BigDecimal("1000.00"));
        assertTrue(financeService.hasNonNegativeBalance(client.getId()));
    }

    @Test
    void hasNonNegativeBalance_shouldReturnTrue_whenBalanceIsZero() {
        saveAccount(BigDecimal.ZERO);
        assertTrue(financeService.hasNonNegativeBalance(client.getId()));
    }

    @Test
    void hasNonNegativeBalance_shouldReturnFalse_whenBalanceIsNegative() {
        saveAccount(new BigDecimal("-1000.00"));
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

    private ClientAccount saveAccount(BigDecimal balance) {
        ClientAccount account = new ClientAccount();
        account.setTenantId(tenant.getId());
        account.setClientId(client.getId());
        account.setBalance(balance);
        return clientAccountRepository.save(account);
    }

    private Invoice saveUnpaidInvoice(BigDecimal amount) {
        Invoice invoice = new Invoice();
        invoice.setTenantId(tenant.getId());
        invoice.setClientMembershipId(clientMembership.getId());
        invoice.setClientId(client.getId());
        invoice.setDueDate(LocalDate.now().minusDays(1));
        invoice.setAmount(amount);
        invoice.setStatus(InvoiceStatus.UNPAID);
        return invoiceRepository.save(invoice);
    }
}
