package org.apereo.cas.aup;

import lombok.val;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.LdapUtils;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.SearchResult;
import org.springframework.webflow.execution.RequestContext;

/**
 * This is {@link LdapAcceptableUsagePolicyRepository}.
 * Examines the principal attribute collection to determine if
 * the policy has been accepted, and if not, allows for a configurable
 * way so that user's choice can later be remembered and saved back into
 * the LDAP instance.
 *
 * @author Misagh Moayyed
 * @since 4.2
 */
@Slf4j
public class LdapAcceptableUsagePolicyRepository extends AbstractPrincipalAttributeAcceptableUsagePolicyRepository {
    private static final long serialVersionUID = 1600024683199961892L;

    private final transient ConnectionFactory connectionFactory;
    private final String searchFilter;
    private final String baseDn;

    public LdapAcceptableUsagePolicyRepository(final TicketRegistrySupport ticketRegistrySupport,
                                               final String aupAttributeName,
                                               final ConnectionFactory connectionFactory,
                                               final String searchFilter, final String baseDn) {
        super(ticketRegistrySupport, aupAttributeName);
        this.connectionFactory = connectionFactory;
        this.searchFilter = searchFilter;
        this.baseDn = baseDn;
    }

    @Override
    public boolean submit(final RequestContext requestContext, final Credential credential) {
        try {
            val response = searchForId(credential.getId());
            if (LdapUtils.containsResultEntry(response)) {
                val currentDn = response.getResult().getEntry().getDn();
                LOGGER.debug("Updating [{}]", currentDn);
                return LdapUtils.executeModifyOperation(currentDn, this.connectionFactory,
                    CollectionUtils.wrap(this.aupAttributeName, CollectionUtils.wrapSet(Boolean.TRUE.toString())));
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * Search for service by id.
     *
     * @param id the id
     * @return the response
     * @throws LdapException the ldap exception
     */
    private Response<SearchResult> searchForId(final String id) throws LdapException {
        val filter = LdapUtils.newLdaptiveSearchFilter(this.searchFilter,
            LdapUtils.LDAP_SEARCH_FILTER_DEFAULT_PARAM_NAME,
            CollectionUtils.wrap(id));
        return LdapUtils.executeSearchOperation(this.connectionFactory, this.baseDn, filter);
    }
}
