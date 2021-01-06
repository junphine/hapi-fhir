package com.satyy.ldap.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.ldap.DefaultLdapUsernameToDnMapper;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.LdapUsernameToDnMapper;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsManager;
import org.springframework.security.web.authentication.logout.LogoutFilter;

import com.satyy.ldap.model.Role;
import com.satyy.ldap.filter.JwtAuthenticationTokenFilter;

/**
 * 
 */
@Configuration
@ConfigurationProperties(prefix = "ldap")
@PropertySource(value = "classpath:ldap.properties")
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${ldap.connection.url}")
    private String ldapConnectionUrl;

    @Value("${ldap.authorization.group.name}")
    private String authGroupName;

    @Autowired
    private LdapGroupProperties groupProperties;

    @Autowired
    private LdapUserProperties userProperties;

    

    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    
    @Override
	protected void configure(HttpSecurity httpSecurity) throws Exception {
		// Disable CSRF
		httpSecurity.csrf().disable()
				// Only admin can perform HTTP delete operation
				.authorizeRequests()
				.antMatchers(HttpMethod.POST).hasRole(Role.USER)
				.antMatchers(HttpMethod.DELETE).hasRole(Role.ADMIN)
				.antMatchers(HttpMethod.PUT).hasRole(Role.MANAGER)
				.antMatchers(HttpMethod.PATCH).hasRole(Role.MANAGER)
				// any authenticated user can perform all other operations
				.antMatchers("/admin","/admin/**").hasAnyRole(Role.ADMIN, Role.MANAGER).and().httpBasic()
				// Permit all other request without authentication
				.and().authorizeRequests().anyRequest().permitAll()
				//.and().antMatcher("authenticate").anonymous()
				// We don't need sessions to be created.
				.and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		
		httpSecurity.addFilterBefore(jwtAuthenticationTokenFilter, LogoutFilter.class); 
	}
    
    @Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

    @Override
    public void configure(final AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder
        		.authenticationProvider(ldapAuthenticationProvider())
                .ldapAuthentication()
                .userSearchBase(userProperties.getBase())
                .userSearchFilter(userProperties.getFilter())
                .userDnPatterns("cn={0},"+userProperties.getBase())
                .ldapAuthoritiesPopulator(ldapAuthoritiesPopulator())
                .contextSource(contextSource())
                .passwordCompare()
                .passwordEncoder(new BCryptPasswordEncoder())
                .passwordAttribute(userProperties.getPasswordAttribute());
    }

    @Bean
    public LdapAuthoritiesPopulator ldapAuthoritiesPopulator() {

        final DefaultLdapAuthoritiesPopulator authoritiesPopulator =
                new DefaultLdapAuthoritiesPopulator(contextSource(),"ou=groups") {

            @Override
            public Set<GrantedAuthority> getGroupMembershipRoles(final String userDn,
                                                                 final String username) {
                final Set<GrantedAuthority> groupMembershipRoles =
                        super.getGroupMembershipRoles(userDn, username);
                boolean isValidMember = true;
                final String authorizeRole = groupProperties.getPrefix() + authGroupName;

                for (GrantedAuthority grantedAuthority : groupMembershipRoles) {
                    if (authorizeRole.equalsIgnoreCase(grantedAuthority.toString())) {
                        isValidMember = true;
                        break;
                    }
                }

                if (!isValidMember) {
                    throw new BadCredentialsException("User must be a member of group - " + authGroupName);
                }
                return groupMembershipRoles;
            }
        };
        authoritiesPopulator.setGroupRoleAttribute(groupProperties.getAttribute());
        authoritiesPopulator.setRolePrefix(groupProperties.getPrefix());
        authoritiesPopulator.setGroupSearchFilter(groupProperties.getFilter());

        return authoritiesPopulator;
    }

    @Bean
    public DefaultSpringSecurityContextSource contextSource() {
    	if(ds==null) {
    	  ds = new DefaultSpringSecurityContextSource(ldapConnectionUrl);
    	//ds.setUserDn(userDn);
    	//ds.setPassword(password);
    	}
    	return ds;
    }
    
    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider() {
    	BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource());    	
    	bindAuthenticator.setUserSearch(new FilterBasedLdapUserSearch(userProperties.getBase(),userProperties.getFilter(),contextSource()));
    	List<String> dnPattens = new ArrayList<>();
    	dnPattens.add("cn={0},"+userProperties.getBase());
    	bindAuthenticator.setUserDnPatterns(dnPattens.toArray(new String[dnPattens.size()]));
    	LdapAuthenticationProvider provider = new LdapAuthenticationProvider(bindAuthenticator,ldapAuthoritiesPopulator());
    	
    	return provider;
    }
    
    @Bean
    public LdapUserDetailsManager ldapUserDetailsManager() {
    	LdapUserDetailsManager userManager = new LdapUserDetailsManager(contextSource());
    	userManager.setGroupMemberAttributeName("member");
    	userManager.setGroupSearchBase("ou=groups");
    	userManager.setGroupRoleAttributeName(groupProperties.getAttribute());
    	
    	LdapUsernameToDnMapper usernameMapper = new DefaultLdapUsernameToDnMapper(userProperties.getBase(), "cn");
    	userManager.setUsernameMapper(usernameMapper);
    	return userManager;
    }
    
    DefaultSpringSecurityContextSource ds;
}