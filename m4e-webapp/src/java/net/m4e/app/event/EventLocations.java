/*
 * Copyright (c) 2017 by Botorabi. All rights reserved.
 * https://github.com/botorabi/Meet4Eat
 * 
 * License: MIT License (MIT), read the LICENSE text in
 *          main directory for more details.
 */

package net.m4e.app.event;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import net.m4e.app.resources.DocumentEntity;
import net.m4e.common.Entities;
import net.m4e.app.resources.StatusEntity;
import net.m4e.common.Strings;
import net.m4e.system.core.AppInfoEntity;
import net.m4e.system.core.AppInfos;
import net.m4e.system.core.Log;

/**
 * A collection of event location related utilities
 *
 * @author boto
 * Date of creation Sep 13, 2017
 */
public class EventLocations {

    /**
     * Used for logging
     */
    private final static String TAG = "EventLocations";

    private final EntityManager entityManager;

    /**
     * Create an instance of event utilities.
     * 
     * @param entityManager    Entity manager
     */
    public EventLocations(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Create a new event location in database.
     * 
     * @param event        The event getting the new location
     * @param inputEntity  Entity containing the new location data
     * @param creatorID    Creator ID
     * @return             A new created event location entity if successfully, otherwise null.
     * @throws Exception   Throws an exception if something goes wrong.
     */
    public EventLocationEntity createNewLocation(EventEntity event, EventLocationEntity inputEntity, Long creatorID) throws Exception {
        // setup the new entity
        EventLocationEntity newlocation = new EventLocationEntity();
        newlocation.setName(inputEntity.getName());
        newlocation.setDescription(inputEntity.getDescription());
        if (inputEntity.getPhoto() != null) {
            updateEventLocationImage(newlocation, inputEntity.getPhoto());
        }

        // setup the status
        StatusEntity status = new StatusEntity();
        status.setIdCreator(creatorID);
        status.setIdOwner(creatorID);
        Date now = new Date();
        status.setDateCreation(now.getTime());
        status.setDateLastUpdate(now.getTime());
        newlocation.setStatus(status);

        Entities eutils = new Entities(entityManager);
        eutils.createEntity(newlocation);
        Collection<EventLocationEntity> locs = event.getLocations();
        if (locs == null) {
            locs = new ArrayList<>();
            event.setLocations(locs);
        }
        event.getLocations().add(newlocation);
        eutils.updateEntity(event);
        return newlocation;
    }

    /**
     * Update the given entity in database.
     * 
     * @param inputLocation  The location containing the updates
     * @return               Updated location
     * @throws Exception     Throws an exception if something went wrong.
     */
    EventLocationEntity updateLocation(EventLocationEntity inputLocation) throws Exception {
        Entities entityutils = new Entities(entityManager);
        EventLocationEntity location = entityutils.findEntity(EventLocationEntity.class, inputLocation.getId());
        if ((location == null) || !location.getStatus().getIsActive()) {
            throw new Exception("Entity location does not exist.");
        }

        if (inputLocation.getName() != null) {
            location.setName(inputLocation.getName());
        }
        if (inputLocation.getDescription() != null) {
            location.setDescription(inputLocation.getDescription());
        }
        if (inputLocation.getPhoto() != null) {
            updateEventLocationImage(location, inputLocation.getPhoto());
        }

        entityutils.updateEntity(location);
        return location;
    }

    /**
     * Update the event location image with the content of given image. The given image
     * is checked in document pool and if there is no such image in pool then a new one
     * will be created.
     * 
     * @param location      Event location entity
     * @param image         Image to set to given event
     * @throws Exception    Throws exception if any problem occurred.
     */
    void updateEventLocationImage(EventLocationEntity location, DocumentEntity image) throws Exception {
        Entities entities = new Entities(entityManager);
        // make sure that the resource URL is set
        image.setResourceURL("/EventLoction/Image");
        entities.updateEntityPhoto(location, image);
    }

    /**
     * Try to find an event location with given user ID.
     * 
     * @param id Event location ID
     * @return Return an entity if found, otherwise return null.
     */
    public EventLocationEntity findLocation(Long id) {
        Entities eutils = new Entities(entityManager);
        EventLocationEntity event = eutils.findEntity(EventLocationEntity.class, id);
        return event;
    }

    /**
     * Mark the given location as deleted.
     * 
     * @param event             Event location to mark
     * @param locationToRemove  Location to mark as deleted
     * @throws Exception        Throws an exception if something went wrong.
     */
    public void markLocationAsDeleted(EventEntity event, EventLocationEntity locationToRemove) throws Exception {
        if (locationToRemove.getStatus().getIsDeleted()) {
            throw new Exception("Location is already deleted.");            
        }
        Collection<EventLocationEntity> locations = event.getLocations();
        if ((locations == null) || !locations.contains(locationToRemove)) {
            throw new Exception("Location is not part of event.");
        }
        // mark the location entity as deleted
        locationToRemove.getStatus().setDateDeletion((new Date()).getTime());
        Entities eutils = new Entities(entityManager);
        eutils.updateEntity(locationToRemove);

        // update the app stats
        AppInfos autils = new AppInfos(entityManager);
        AppInfoEntity appinfo = autils.getAppInfoEntity();
        if (appinfo == null) {
            throw new Exception("Problem occured while retrieving AppInfo entity!");
        }
        appinfo.incrementEventLocationCountPurge(1L);
        eutils.updateEntity(appinfo);
    }

    /**
     * Given a JSON string, import the necessary fields and create an event location entity.
     * 
     * @param jsonString  JSON string representing an event location entity
     * @return            Event location entity or null if the JSON string was not appropriate
     */
    public EventLocationEntity importLocationJSON(String jsonString) {
        if (jsonString == null) {
            return null;
        }

        String name, idstring, description, photo;
        try {
            JsonReader jreader = Json.createReader(new StringReader(jsonString));
            JsonObject jobject = jreader.readObject();
            idstring    = jobject.getString("id", "");
            name        = jobject.getString("name", null);
            description = jobject.getString("description", null);
            photo       = jobject.getString("photo", null);
        }
        catch(Exception ex) {
            Log.warning(TAG, "Could not setup an event loaction given JSON string, reason: " + ex.getLocalizedMessage());
            return null;
        }

        long id = 0;
        try {
            id = Long.parseLong(idstring);
        }
        catch(NumberFormatException ex) {}

        EventLocationEntity entity = new EventLocationEntity();
        if (id != 0) {
            entity.setId(id);
        }
        if (name != null) {
            entity.setName(Strings.limitStringLen(name, 32));
        }
        if (description != null) {
            entity.setDescription(Strings.limitStringLen(description, 1000));
        }

        if (photo != null) {
            DocumentEntity image = new DocumentEntity();
            // currently we expect only base64 encoded images here
            image.setEncoding(DocumentEntity.ENCODING_BASE64);
            image.updateContent(photo.getBytes());
            image.setType(DocumentEntity.TYPE_IMAGE);
            entity.setPhoto(image);
        }

        return entity;
    }

    /**
     * Given an event location entity, export the necessary fields into a JSON object.
     * 
     * @param entity    Event location entity to export
     * @return          A JSON object containing builder the proper entity fields
     */
    public JsonObjectBuilder exportEventLocationJSON(EventLocationEntity entity) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("id", (entity.getId() != null) ? entity.getId().toString() : "")
            .add("name", (entity.getName() != null) ? entity.getName() : "")
            .add("description", (entity.getDescription() != null) ? entity.getDescription(): "")
            .add("photoId", (entity.getPhoto() != null) ? entity.getPhoto().getId().toString() : "")
            .add("photoETag", (entity.getPhoto() != null) ? entity.getPhoto().getETag() : "");
        return json;
    }
}
