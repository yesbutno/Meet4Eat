/**
 * Copyright (c) 2017 by Botorabi. All rights reserved.
 * https://github.com/botorabi/Meet4Eat
 *
 * License: MIT License (MIT), read the LICENSE text in
 *          main directory for more details.
 */

#ifndef REST_EVENT_H
#define REST_EVENT_H

#include <configuration.h>
#include <webapp/m4e-api/m4e-rest.h>
#include <user/modeluser.h>
#include <event/modelevent.h>
#include <event/modellocationvotes.h>


namespace m4e
{
namespace webapp
{

/**
 * @brief Class handling the event related web app interaction
 *
 * @author boto
 * @date Sep 13, 2017
 */
class RESTEvent : public Meet4EatREST
{
    /**
     * @brief TAG Used for logging
     */
    const std::string TAG = "(RESTEvent) ";

    Q_OBJECT

    public:

        /**
         * @brief Construct an instance.
         *
         * @param p_parent Parent instance
         */
        explicit                RESTEvent( QObject* p_parent );

        /**
         * @brief Destroy instance.
         */
        virtual                 ~RESTEvent();

        /**
         * @brief Get all user events. The results are emitted by signal 'onRESTEventGetEvents'.
         *
         * @param userId   The user ID
         */
        void                    getEvents();

        /**
         * @brief Get user's event with given ID. The results are emitted by signal 'onRESTEventGetEvent'.
         *
         * @param eventId  The event ID
         */
        void                    getEvent( const QString& eventId );

        /**
         * @brief Create a new event. The results are emitted by signal 'onRESTEventNewEvent'.
         *
         * @param event Event to create
         */
        void                    createEvent( m4e::event::ModelEventPtr event );

        /**
         * @brief Delete user's event with given ID. The results are emitted by signal 'onRESTEventDeleteEvent'.
         *
         * @param eventId  The event ID
         */
        void                    deleteEvent( const QString& eventId );

        /**
         * @brief Update an existing event. The results are emitted by signal 'onRESTEventUpdateEvent'.
         *
         * @param event Event to upate
         */
        void                    updateEvent( m4e::event::ModelEventPtr event );

        /**
         * @brief Add a new member to event.
         *
         * @param eventId   The event ID
         * @param memberId  The user ID
         */
        void                    addMember( const QString& eventId, const QString& memberId );

        /**
         * @brief Remove a member from event.
         *
         * @param eventId   The event ID
         * @param memberId  The user ID
         */
        void                    removeMember( const QString& eventId, const QString& memberId );

        /**
         * @brief Get event  location data.
         *
         * @param eventId     The event ID
         * @param locationId  The location ID
         */
        void                    getLocation( const QString& eventId, const QString& locationId );

        /**
         * @brief Add a new location to event.
         *
         * @param eventId   The event ID
         * @param location  The location ID
         */
        void                    addLocation( const QString& eventId, event::ModelLocationPtr location );

        /**
         * @brief Remove a location from given event.
         *
         * @param eventId     The event ID
         * @param locationId  ID of location to remove
         */
        void                    removeLocation( const QString& eventId, const QString& locationId );

        /**
         * @brief Update event location.
         *
         * @param eventId     The event ID
         * @param locationId  ID of location to update
         */
        void                    updateLocation( const QString& eventId, event::ModelLocationPtr location );

        /**
         * @brief Set a vote for an event location.
         *
         * @param eventId     The event ID
         * @param locationId  The location ID
         * @param vote        Pass true in order to vote for location, false for unvote it
         */
        void                    setLocationVote( const QString& eventId, const QString& locationId, bool vote );

        /**
         * @brief Get votes of all event locations for a given time range.
         *
         * @param eventId       The event ID
         * @param timeBegin     Time range begin
         * @param timeEnd       Time range end
         */
        void                    getLocationVotesByTime( const QString& eventId, const QDateTime& timeBegin, const QDateTime& timeEnd );

        /**
         * @brief Get votes of a single event location.
         *
         * @param votesId       The ID of location votes entry
         */
        void                    getLocationVotesById( const QString& votesId );

    signals:

        /**
         * @brief Emit the results of getUserEvents request.
         *
         * @param events    User events
         */
        void                    onRESTEventGetEvents( QList< m4e::event::ModelEventPtr > events );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorGetEvents( QString errorCode, QString reason );

        /**
         * @brief Emit the results of getEvent request.
         *
         * @param event    User event
         */
        void                    onRESTEventGetEvent( m4e::event::ModelEventPtr event );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorGetEvent( QString errorCode, QString reason );

        /**
         * @brief Emit the results of newEvent request.
         *
         * @param eventId   ID of new event
         */
        void                    onRESTEventNewEvent( QString eventId );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorNewEvent( QString errorCode, QString reason );

        /**
         * @brief Emit the results of deleteEvent request.
         *
         * @param eventId   ID of deleted event
         */
        void                    onRESTEventDeleteEvent( QString eventId );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorDeleteEvent( QString errorCode, QString reason );

        /**
         * @brief Emit the results of updateEvent request.
         *
         * @param eventId   ID of updated event
         */
        void                    onRESTEventUpdateEvent( QString eventId );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorUpdateEvent( QString errorCode, QString reason );

        /**
         * @brief Emit the results of addMember request.
         *
         * @param eventId   ID of event the user was added to
         * @param memberId  User ID of new member
         */
        void                    onRESTEventAddMember( QString eventId, QString memberId );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorAddMember( QString errorCode, QString reason );

        /**
         * @brief Emit the results of removeMember request.
         *
         * @param eventId   ID of event the user removed from
         * @param memberId  User ID of removed member
         */
        void                    onRESTEventRemoveMember( QString eventId, QString memberId );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorRemoveMember( QString errorCode, QString reason );


        /**
         * @brief Signal is emitted when the results of getLocation request arrive.
         *
         * @param location    The location
         */
        void                    onRESTEventGetLocation( m4e::event::ModelLocationPtr location );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorGetLocation( QString errorCode, QString reason );

        /**
         * @brief Emit the results of addLocation request.
         *
         * @param eventId     ID of event the location was added to
         * @param locationId  ID of new location
         */
        void                    onRESTEventAddLocation( QString eventId, QString locationId );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorAddLocation( QString errorCode, QString reason );

        /**
         * @brief Emit the results of removeLocation request.
         *
         * @param eventId     ID of event the location was removed from
         * @param locationId  ID of removed location
         */
        void                    onRESTEventRemoveLocation( QString eventId, QString locationId );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorRemoveLocation( QString errorCode, QString reason );

        /**
         * @brief Emit the results of updateLocation request.
         *
         * @param eventId     ID of event containing the location
         * @param locationId  ID of updated location
         */
        void                    onRESTEventUpdateLocation( QString eventId, QString locationId );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorUpdateLocation( QString errorCode, QString reason );

        /**
         * @brief Emit the results of setLocationVote request.
         *
         * @param eventId     ID of event containing the location
         * @param locationId  ID of updated location
         * @param votesId     ID of the votes entry
         * @param vote        true for vote, false for unvote
         */
        void                    onRESTEventSetLocationVote( QString eventId, QString locationId, QString votesId, bool vote );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorSetLocationVote( QString errorCode, QString reason );

        /**
         * @brief Emit the results of getLocationVotesByTime request.
         *
         * @param votes     The location votes
         */
        void                    onRESTEventGetLocationVotesByTime( QList<  m4e::event::ModelLocationVotesPtr > votes );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorGetLocationVotesByTime( QString errorCode, QString reason );

        /**
         * @brief Emit the results of getLocationVotesById request.
         *
         * @param votes     The location votes for requested location
         */
        void                    onRESTEventGetLocationVotesById( m4e::event::ModelLocationVotesPtr votes );

        /**
         * @brief Signal is emitted when there were a problem communicating to server or the results status were not ok.
         *
         * @param errorCode Error code if any exits
         * @param reason    Error string
         */
        void                    onRESTEventErrorGetLocationVotesById( QString errorCode, QString reason );
};

} // namespace webapp
} // namespace m4e

#endif // REST_EVENT_H
