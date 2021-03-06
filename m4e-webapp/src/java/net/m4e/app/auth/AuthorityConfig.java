/*
 * Copyright (c) 2017 by Botorabi. All rights reserved.
 * https://github.com/botorabi/Meet4Eat
 * 
 * License: MIT License (MIT), read the LICENSE text in
 *          main directory for more details.
 */
package net.m4e.app.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import net.m4e.system.core.Log;
import net.m4e.app.user.UserEntity;

/**
 * Central place for holding all authority related configuration
 * 
 * @author boto
 * Date of creation Aug 22, 2017
 */
public class AuthorityConfig {

    /**
     * Used for logging
     */
    private final static String TAG = "AuthorityConfig";

    /**
     * Session attribute name for user.
     */
    public static final String SESSION_ATTR_USER = "USER";

    /**
     * A list of java beans which should be unter control of authority checker.
     * Extend the list whenever new REST beans are created which need protected access.
     */
    private final Class[] accessBeanClasses = {
        net.m4e.system.core.AppInfoEntityFacadeREST.class,
        net.m4e.system.maintenance.MaintenanceFacadeREST.class,
        net.m4e.app.user.UserEntityFacadeREST.class,
        net.m4e.app.user.UserAuthenticationFacadeREST.class,
        net.m4e.app.event.EventEntityFacadeREST.class,
        net.m4e.app.event.EventLocationVoteEntityFacadeREST.class,
        net.m4e.app.resources.DocumentEntityFacadeREST.class,
        net.m4e.app.mailbox.MailEntityFacadeREST.class
    };

    /**
     * Count of iterations for creating a hash.
     */
    private static final int PW_HASH_ITERATOIN = 10;

    /**
     * Construct the instance.
     */
    private AuthorityConfig() {}

    /**
     * Given a HTTP request object, return the user entity set in its session.
     * 
     * @param request   HTTP request
     * @return          Return session's UserEntity, or null if no user was set in session
     */
    public UserEntity getSessionUser(HttpServletRequest request) {
       return getSessionUser(request.getSession());
    }

    /**
     * Given a HTTP session, return the user entity set in its session.
     * 
     * @param session   HTTP session
     * @return          Return session's UserEntity, or null if no user was set in session
     */
    public UserEntity getSessionUser(HttpSession session) {
        Object sessionuser = session.getAttribute(AuthorityConfig.SESSION_ATTR_USER);
        if ((sessionuser == null) || !(sessionuser instanceof UserEntity)) {
            return null;
        }
        return (UserEntity)sessionuser;
    }

    /**
     * Get the single instance of authority configuration.
     * 
     * @return Authority configuration
     */
    public static AuthorityConfig getInstance() {
        return AuthorityConfigHolder.INSTANCE;
    }

    /**
     * Get a list with all bean classes which need protected resource access.
     * 
     * @return List of bean classes.
     */
    public List<Class> getAccessBeanClasses() {
        return Arrays.asList(accessBeanClasses);
    }

    /**
     * Get the names of default permissions.
     * 
     * @return List of default permission names
     */
    public List<String> getDefaultPermissions() {
        return new DefaultPermissions().getPermissionNames();
    }

    /**
     * Get all default roles and their permissions.
     * 
     * @return All default roles and their permissions
     */
    public Map<String, List<String>> getDefaultRoles() {
        return new DefaultRoles().getRoles();
    }

    /**
     * Given a password, create a representing hash using SHA-512.
     * 
     * @param string    String to build hash for
     * @return          Hash string, null if something went wrong
     */
    public String createPassword(String string) {
        String pw = "" + string;
        for (int i = 0; i < PW_HASH_ITERATOIN; i++) {
            pw = createHash(pw);
            if (pw == null) {
                return null;
            }
        }
        return pw;
    }

    /**
     * Given a string, create a representing hash using SHA-512.
     * 
     * @param string    String to build hash for
     * @return          Hash string, null if something went wrong
     */
    public String createHash(String string) {
        String res = null;
        try {
            MessageDigest diggest = MessageDigest.getInstance("SHA-512");
            diggest.update(string.getBytes());
            byte data[] = diggest.digest();
            StringBuilder hexstring = new StringBuilder();
            for (int i=0; i < data.length; i++) {
                String hex = Integer.toHexString(0xff & data[i]);
                if (hex.length() == 1) {
                    hexstring.append('0');
                }
                hexstring.append(hex);
            }
            res = hexstring.toString();
        }
        catch (NoSuchAlgorithmException ex) {
            Log.error(TAG, "Problem occurred while hashing a string, reason: " + ex.getLocalizedMessage());
        }
        return res;
    }

    /**
     * Holder for the single instance.
     */
    private static class AuthorityConfigHolder {
        private static final AuthorityConfig INSTANCE = new AuthorityConfig();
    }
}
