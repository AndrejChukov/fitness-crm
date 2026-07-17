package ru.fitnesscrm.common.tenant;

import ru.fitnesscrm.identity.domain.Role;

import java.util.function.Supplier;

public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Role> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> BYPASS_FILTER = ThreadLocal.withInitial(() -> false);

    private TenantContext() {
    }

    public static void set(Long tenantId, Long userId, Role role) {
        TENANT_ID.set(tenantId);
        USER_ID.set(userId);
        ROLE.set(role);
    }

    public static void setBypassFilter(Boolean filter) {
        BYPASS_FILTER.set(filter);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static Role getRole() {
        return ROLE.get();
    }

    public static boolean isSuperAdmin() {
        return Role.SUPER_ADMIN.equals(ROLE.get());
    }

    public static boolean shouldApplyTenantFilter() {
        return !isSuperAdmin() && TENANT_ID.get() != null && !BYPASS_FILTER.get();
    }

    public static <T> T executeWithoutFilter(Supplier<T> supplier) {
        setBypassFilter(true);
        try {
            return supplier.get();
        } finally {
            setBypassFilter(false);
        }
    }


    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        ROLE.remove();
        BYPASS_FILTER.remove();
    }
}
