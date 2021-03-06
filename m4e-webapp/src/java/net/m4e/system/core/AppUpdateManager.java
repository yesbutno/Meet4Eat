/*
 * Copyright (c) 2017 by Botorabi. All rights reserved.
 * https://github.com/botorabi/Meet4Eat
 * 
 * License: MIT License (MIT), read the LICENSE text in
 *          main directory for more details.
 */
package net.m4e.system.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import net.m4e.common.Entities;

/**
 * This class cares about application updates. Whenever a new app version
 * is deployed this class deals with necessary steps to update data structures
 * etc. The update manager is used during application startup.
 * 
 * NOTE: Every deployed application version must have an own registered updater
 *       in order to handle incremental updates properly. See UpdateResistry class.
 * 
 * @author boto
 * Date of creation Aug 22, 2017
 */
public class AppUpdateManager {

    /**
     * Used for logging
     */
    private final static String TAG = "AppUpdateManager";

    /**
     * Entity manager is used for accessing the database.
     */
    private final EntityManager    entityManager;

    /**
     * List of all available update instances. See also class AppUpdateRegistry.
     */
    private final List<AppUdateBaseHandler> updateRegistry = new ArrayList();

    /**
     * Construct the update manager.
     * 
     * @param entityManager   Entity manager
     */
    public AppUpdateManager(EntityManager entityManager) {
        this.entityManager   = entityManager;
    }

    /**
     * Register an instance of an update class. The manager selects a proper update class basing on current deployment.
     * The registration of all update classes happens when needed in method checkForUpdate.
     * 
     * @param updater   Updater instance
     */
    protected void registerUpdater(AppUdateBaseHandler updater) {    
        updateRegistry.add(updater);
    }

    /**
     * Check if a new version of the application was deployed. If so then take the
     * necessary steps for finalizing the update.
     * 
     * @param appVersion Application version as found in deployment descriptor such as web.xml
     */
    public void checkForUpdate(String appVersion) {
        // register all available updaters
        AppUpdateRegistry ur = new AppUpdateRegistry();
        ur.registerAllUpdaters(this);

        // sort all updates considering their incremental numbers
        sortUpdateRegistry();

        Entities eutils = new Entities(entityManager);
        List<AppInfoEntity> res = eutils.findAllEntities(AppInfoEntity.class);
        // check if there is already an app info entry in database
        if (res.size() > 0) {
            if (res.size() > 1) {
                Log.error(TAG, "*** Error while checking the app info. More than one app info entry detected in database!");
                return;
            }

            AppInfoEntity info = (AppInfoEntity) res.get(0);
            String currentversion = info.getVersion();
            // app version was changed?
            if (!currentversion.contentEquals(appVersion)) {
                Log.info(TAG, "A new version of application was deployed.");
                if (performUpdate(currentversion, appVersion)) {
                    updateAppVersionEntry(info, appVersion);
                }
            }
        }
        else { // perform the initialization of a new installation)
            Log.info(TAG, "Initial version of application was deployed.");
            if (performInitialUpdate()) {
                createAppVersionEntry(appVersion);
            }
            else {
                Log.warning(TAG, "*** Coult not perform the initial deployment setup!");
            }
        }
    }

    /**
     * Sort all registered update instances. This allows several incremental updates
     * in one go, if needed.
     */
    private void sortUpdateRegistry() {
        updateRegistry.sort((Object left, Object right) -> {
            int incnumleft  = ((AppUdateBaseHandler)left).getIncUpdateNumber();
            int incnumright = ((AppUdateBaseHandler)right).getIncUpdateNumber();
            return incnumleft - incnumright;
        });
    }

    /**
     * Create an entry in database, this will be done only for the very first time.
     * 
     * @param appVersion The application version
     */
    private void createAppVersionEntry(String appVersion) {
        AppInfoEntity info = new AppInfoEntity();
        info.setVersion(appVersion);
        info.setDataLastUpdate((new Date().getTime()));
        AppInfos autils = new AppInfos(entityManager);
        Entities eutils = new Entities(entityManager);
        try {
            eutils.createEntity(info);
        }
        catch (Exception ex) {
            Log.error(TAG, "*** Problem occured while creating app information in database, reason: " + ex.getLocalizedMessage());
        }
    }

    /**
     * Update the info entry in database.
     * 
     * @param info          Entity representing the app info entry in database
     * @param appVersion    The new app version which is updated in entry
     */
    private void updateAppVersionEntry(AppInfoEntity info, String appVersion) {
        info.setVersion(appVersion);
        info.setDataLastUpdate((new Date().getTime()));
        AppInfos autils = new AppInfos(entityManager);
        autils.updateAppInfoEntity(info);
    }

    /**
     * Perform an update from current version to a new version.
     * 
     * @param currentVersion    Current application version read from database
     * @param newVersion        New application version read from deployment descriptor
     * @return                  Return true if the update was successful, otherwise false.
     */
    private boolean performUpdate(String currentVersion, String newVersion) {
        Log.info(TAG, "Start updating deployment...");
        int indexcurrent = updateRegistry.indexOf(findUpdater(currentVersion));
        int indexnew = updateRegistry.indexOf(findUpdater(newVersion));
        // check if there is an updater for this version
        if (indexnew < 0) {
            Log.debug(TAG, "There is no need for update migration for this version: " + currentVersion);
            Log.info(TAG, "Deployment updating successfully completed");
            return true;
        }

        if (indexcurrent < 0) {
            Log.warning(TAG, "   Current version had no updater, update to new version skipping potention versions in between!");
            indexcurrent = indexnew;
        }

        // do some deployment consistency checks
        indexcurrent++;
        if (indexcurrent > indexnew) {
            Log.error(TAG, "*** New updater is older than the current updater (" + indexcurrent + " > " + indexnew + "). Check the deployed package, it may contain errors.");
            return false;
        }
        // go through every incremental update up to the current version
        for (int i = indexcurrent; i <= indexnew; i++) {
            Log.info(TAG, "  Start updating to version " + updateRegistry.get(i).getAppVersion());
            try {
                updateRegistry.get(i).performUpdate(entityManager);
                Log.info(TAG, "  Successfully updated to version " + updateRegistry.get(i).getAppVersion());
            }
            catch (Exception ex) {
                Log.error(TAG, "***  Failed to perform the deployment update, reason: " + ex.getLocalizedMessage());
                return false;
            }
        }
        Log.info(TAG, "Deployment updating successfully completed");
        return true;
    }

    /**
     * Perform the initial update during the very first deployment (installation).
     * Here the special updater 'UpdateInit' will be used.
     * 
     * @return                  Return true if the update was successful, otherwise false.
     */
    private boolean performInitialUpdate() {
        // NOTE updater version "0.0.0" is a special updater for initial setup, see class 'UpdateInit'.
        AppUdateBaseHandler updater = findUpdater("0.0.0");
        if (updater == null) {
            return false;
        }
        try {
            updater.performUpdate(entityManager);
            Log.info(TAG, "  Successfully updated to initial version " + updater.getAppVersion());
        }
        catch (Exception ex) {
            Log.error(TAG, "***  Failed to perform the initial deployment update, reason: " + ex.getLocalizedMessage());
            return false;
        }
        return true;
    }

    /**
     * Try to find a registered deployment updater given an app version.
     * 
     * @param appVersion    Application version the update instance belongs to
     * @return              Updater instance, or null if no one exists for given
     *                      app version
     */
    private AppUdateBaseHandler findUpdater(String appVersion) {
        for (AppUdateBaseHandler u: updateRegistry) {
            if (u.getAppVersion().equals(appVersion)) {
                return u;
            }
        }
        return null;
    }
}
