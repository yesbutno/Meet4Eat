/*
 * Copyright (c) 2017 by Botorabi. All rights reserved.
 * https://github.com/botorabi/Meet4Eat
 * 
 * License: MIT License (MIT), read the LICENSE text in
 *          main directory for more details.
 */

package net.m4e.app.event;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import net.m4e.app.resources.ImageEntity;

/**
 * This entity describes an event location.
 * 
 * @author abotorabi
 * Date of creation Aug 31, 2017
 */
@Entity
public class EventLocationEntity implements Serializable {

    /**
     * Serialization version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Unique entity ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * Event name
     */
    private String name;

    /**
     * Event description
     */
    private String description;

    /**
     * Photo
     */
    @OneToOne(optional=true, cascade = CascadeType.ALL)
    private ImageEntity photo;

    /**
     * Create an event location instance.
     */
    public EventLocationEntity() {
    }

    /**
     * Get event location ID.
     * 
     * @return Event location ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Set event location ID.
     * 
     * @param id Event location ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get event location name.
     * 
     * @return Event location name
     */
    public String getName() {
        return name;
    }

    /**
     * Set event location name.
     * 
     * @param name Event location name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get event location description.
     * 
     * @return Event location description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set event location description.
     * 
     * @param description Event location description 
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get event location photo.
     * 
     * @return ImageEntity containing the photo
     */
    public ImageEntity getPhoto() {
        return photo;
    }

    /**
     * Set event location photo.
     * 
     * @param photo ImageEntity containing the photo
     */
    public void setPhoto(ImageEntity photo) {
        this.photo = photo;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EventLocationEntity)) {
            return false;
        }
        EventLocationEntity other = (EventLocationEntity) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "net.m4e.events.EventLocationEntity[ id=" + id + " ]";
    }
}