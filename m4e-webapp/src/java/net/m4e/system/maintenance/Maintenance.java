/*
 * Copyright (c) 2017 by Botorabi. All rights reserved.
 * https://github.com/botorabi/Meet4Eat
 * 
 * License: MIT License (MIT), read the LICENSE text in
 *          main directory for more details.
 */

package net.m4e.system.maintenance;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import net.m4e.app.event.EventEntity;
import net.m4e.app.event.EventLocationEntity;
import net.m4e.app.event.Events;
import net.m4e.app.resources.DocumentPool;
import net.m4e.app.user.UserEntity;
import net.m4e.app.user.UserRegistrations;
import net.m4e.app.user.Users;
import net.m4e.common.Entities;
import net.m4e.system.core.AppInfoEntity;
import net.m4e.system.core.AppInfos;
import net.m4e.system.core.Log;


/**
 * Maintenance contains the app maintenance related functionality.
 * 
 * @author boto
 * Date of creation Sep 8, 2017
 */
public class Maintenance {

    /**
     * Used for logging
     */
    private final static String TAG = "Maintenance";

    private final EntityManager entityManager;

    /**
     * Create the instance for given entity manager.
     * 
     * @param entityManager   Entity manager
     */
    public Maintenance(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Give an app info entity export the necessary fields into a JSON object.
     * 
     * @param entity    App info entity to export
     * @return          A JSON object containing builder the proper entity fields
     */
    public JsonObjectBuilder exportInfoJSON(AppInfoEntity entity) {
        UserRegistrations regs = new UserRegistrations(entityManager);
        int pendingaccounts = regs.getCountPendingAccountActivations();
        int pendingpwresets = regs.getCountPendingPasswordResets();

        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("version", entity.getVersion())
            .add("dateLastMaintenance", entity.getDateLastMaintenance())
            .add("dateLastUpdate", entity.getDateLastUpdate())
            .add("userCountPurge", entity.getUserCountPurge())
            .add("eventCountPurge", entity.getEventCountPurge())
            .add("eventLocationCountPurge", entity.getEventLocationCountPurge())
            .add("pendingAccountRegistration", pendingaccounts)
            .add("pendingPasswordResets", pendingpwresets);
        return json;
    }

    /**
     * Purge all resources which are expired, such as account registrations or
     * password reset requests which passed their expiration duration.
     * 
     * @return Count of purged resources
     */
    public int purgeExpiredResources() {
        UserRegistrations regutils = new UserRegistrations(entityManager);
        return regutils.purgeExpiredRequests();
    }

    /**
     * Purge all resources and update the app info by resetting the purge counters
     * and updating "last maintenance time".
     * 
     * @return Count of purged resources
     */
    public int purgeAllResources() {
        int countpurges = purgeExpiredResources();
        countpurges += purgeDeletedResources();
        updateAppInfo();
        return countpurges;
    }

    /**
     * Purge all resources which are marked as deleted.
     * 
     * @return Count of purged resources
     */
    private int purgeDeletedResources() {
        Users        userutils   = new Users(entityManager);
        Events       eventutils  = new Events(entityManager);
        Entities     entityutils = new Entities(entityManager);
        DocumentPool imagepool   = new DocumentPool(entityManager);

        int countpurges = 0;

        List<UserEntity>  users  = userutils.getMarkedAsDeletedUsers();
        List<EventEntity> events = entityutils.findAllEntities(EventEntity.class);

        // first purge dead events and make sure that dead users are removed from remaining events
        for (EventEntity event: events) {
            try {
                if (event.getStatus().getIsDeleted()) {
                    if (event.getPhoto() != null) {
                        imagepool.releasePoolDocument(event.getPhoto());
                    }
                    Collection<EventLocationEntity> locs = event.getLocations();
                    if (locs != null) {
                        // delete the locations
                        locs.stream()
                            .filter((loc) -> (loc.getPhoto() != null))
                            .forEachOrdered((loc) -> {
                                imagepool.releasePoolDocument(loc.getPhoto());
                            });
                    }
                    eventutils.deleteEvent(event);
                    countpurges++;
                }
                else {
                    // remove dead members
                    eventutils.removeAnyMember(event, users);
                    // purge deleted event locations
                    Collection<EventLocationEntity> locs = event.getLocations();
                    if (locs != null) {
                        Predicate<EventLocationEntity> pred = ev-> ev.getStatus().getIsDeleted();
                        List<EventLocationEntity> deadlocs = locs.stream().filter(pred).collect(Collectors.toList());
                        // update event's location list
                        locs.removeAll(deadlocs);
                        entityutils.updateEntity(event);
                        // delete the locations
                        for (EventLocationEntity loc: deadlocs) {
                            entityutils.deleteEntity(loc);
                        }
                        countpurges += deadlocs.size();
                    }
                }
            }
            catch(Exception ex) {
                Log.warning(TAG, "Could not delete event: " + event.getId() + ", name: " +
                            event.getName() + ", reason: " + ex.getLocalizedMessage());
            }
        }
        // now remove all dead users
        for (UserEntity user: users) {
            try {
                if (user.getPhoto() != null) {
                    imagepool.releasePoolDocument(user.getPhoto());
                }
                userutils.deleteUser(user);
                countpurges++;
            }
            catch(Exception ex) {
                Log.warning(TAG, "Could not delete user: " + user.getId() + ", name: " +
                            user.getName() + ", reason: " + ex.getLocalizedMessage());
            }
        }
        return countpurges;
    }

    /**
     * Update the app info. It updates the purge counters and
     * the "last maintenance time".
     */
    public void updateAppInfo() {
        // update app info
        AppInfos autils = new AppInfos(entityManager);
        AppInfoEntity info = autils.getAppInfoEntity();
        if (info == null) {
            Log.warning(TAG, "Could not update app info");
            return;
        }

        // update the purge counters
        Users   userutils      = new Users(entityManager);
        Events  eventutils     = new Events(entityManager);
        int     purgeusers     = userutils.getMarkedAsDeletedUsers().size();
        int     purgeevents    = eventutils.getMarkedAsDeletedEvents().size();
        int     purgeeventlocs = eventutils.getMarkedAsDeletedEventLocations().size();

        info.setEventCountPurge(new Long(purgeevents));
        info.setEventLocationCountPurge(new Long(purgeeventlocs));
        info.setUserCountPurge(new Long(purgeusers));
        info.setDateLastMaintenance((new Date().getTime()));
        autils.updateAppInfoEntity(info);
    }
}
