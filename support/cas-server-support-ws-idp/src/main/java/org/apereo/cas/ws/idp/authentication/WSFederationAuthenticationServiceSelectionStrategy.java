package org.apereo.cas.ws.idp.authentication;

import lombok.val;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apereo.cas.authentication.AuthenticationServiceSelectionStrategy;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.ws.idp.WSFederationConstants;
import org.springframework.core.Ordered;

import java.util.Optional;

/**
 * This is {@link WSFederationAuthenticationServiceSelectionStrategy}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Slf4j
public class WSFederationAuthenticationServiceSelectionStrategy implements AuthenticationServiceSelectionStrategy {
    private static final long serialVersionUID = 8035218407906419228L;

    private final int order = Ordered.HIGHEST_PRECEDENCE;
    private final transient ServiceFactory webApplicationServiceFactory;

    public WSFederationAuthenticationServiceSelectionStrategy(final ServiceFactory webApplicationServiceFactory) {
        this.webApplicationServiceFactory = webApplicationServiceFactory;
    }

    @Override
    public Service resolveServiceFrom(final Service service) {
        if (service != null) {
            val serviceReply = getReplyAsParameter(service).get().getValue();
            LOGGER.debug("Located service id [{}] from service authentication request at [{}]", serviceReply, service.getId());
            return this.webApplicationServiceFactory.createService(serviceReply);
        }
        return service;
    }

    @Override
    public boolean supports(final Service service) {
        return service != null && getRealmAsParameter(service).isPresent() && getReplyAsParameter(service).isPresent();
    }

    private static Optional<NameValuePair> getRealmAsParameter(final Service service) {
        try {
            val builder = new URIBuilder(service.getId());
            final Optional param = builder.getQueryParams()
                    .stream()
                    .filter(p -> p.getName().equals(WSFederationConstants.WTREALM))
                    .findFirst();
            return param;
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    private static Optional<NameValuePair> getReplyAsParameter(final Service service) {
        try {
            val builder = new URIBuilder(service.getId());
            final Optional param = builder.getQueryParams()
                    .stream()
                    .filter(p -> p.getName().equals(WSFederationConstants.WREPLY))
                    .findFirst();
            return param;
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public int getOrder() {
        return this.order;
    }
}
