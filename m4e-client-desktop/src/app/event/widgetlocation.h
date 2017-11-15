/**
 * Copyright (c) 2017 by Botorabi. All rights reserved.
 * https://github.com/botorabi/Meet4Eat
 *
 * License: MIT License (MIT), read the LICENSE text in
 *          main directory for more details.
 */

#ifndef WIDGETLOCATION_H
#define WIDGETLOCATION_H

#include <configuration.h>
#include <webapp/webapp.h>
#include <event/modellocation.h>
#include <event/modellocationvotes.h>
#include <QWidget>
#include <QLabel>


namespace Ui {
    class WidgetLocation;
}

namespace m4e
{
namespace event
{

/**
 * @brief Widget class for an event location
 *
 * @author boto
 * @date Sep 19, 2017
 */
class WidgetLocation : public QWidget
{
    /**
     * @brief TAG Used for logging
     */
    const std::string TAG = "(WidgetLocation) ";

    Q_OBJECT

    public:

        /**
         * @brief Create a new event location widget
         *
         * @param p_webApp  Web application interface
         * @param p_parent  Parent widget
         */
        explicit                    WidgetLocation( webapp::WebApp* p_webApp, QWidget* p_parent = nullptr );

        /**
         * @brief Destroy the instance.
         */
        virtual                     ~WidgetLocation();

        /**
         * @brief Setup the widget for given event location.
         *
         * @param event         The event
         * @param location      Event location
         * @param userIsOwner   Is the authenticated user also the event owner? Some operations are permitted only to owner.
         */
        void                        setupUI( event::ModelEventPtr event, event::ModelLocationPtr location, bool userIsOwner );

        /**
         * @brief Get the location ID.
         *
         * @return ID
         */
        const QString&              getId() const { return _location->getId(); }

        /**
         * @brief Update the votes of this location. Pass an empty object in order to reset the votes UI.
         *
         * @param votes Location votes
         */
        void                        updateVotes( event::ModelLocationVotesPtr votes );

        /**
         * @brief Update widget's voting UI considering the event voting time window.
         * While outside the voting time window the UI will disable the voting related button clicks.
         */
        void                        updateVotingUI();

    signals:

        /**
         * @brief Emitted when the user clicks on the widget.
         *
         * @param id   The location ID
         */
        void                        onClicked( QString id );

        /**
         * @brief Emitted when the user clicks "location delete" button.
         *
         * @param id   The location ID
         */
        void                        onDeleteLocation( QString id );

    protected slots:

        void                        onBtnEditClicked();

        void                        onBtnDeleteClicked();

        void                        onBtnInfoClicked();

        void                        onBtnVoteUpClicked();

        void                        onBtnVoteDownClicked();

        /**
         * @brief This signal is received from webapp when a requested document was arrived.
         *
         * @param document   Document
         */
        void                        onDocumentReady( m4e::doc::ModelDocumentPtr document );


        /**
         * @brief Signal is received when the results of location voting arrive.
         *
         * @param success    true if user data could successfully be retrieved, otherwise false
         * @param eventId    ID of event
         * @param locationId ID of location to update
         * @param votesId    ID of location vote entry
         * @param vote       true for voted for location, false for unvoted location
         */
        void                        onResponseSetLocationVote( bool success, QString eventId, QString locationId, QString votesId, bool vote );

        /**
         * @brief  Signal is received when the results of location votes request by votes ID arrive.
         *
         * @param success   true if user votes could successfully be retrieved, otherwise false
         * @param votes     The event location votes
         */
        void                        onResponseGetLocationVotesById( bool success, m4e::event::ModelLocationVotesPtr votes );

    protected:

        bool                        eventFilter( QObject* p_obj, QEvent* p_event );

        void                        requestSetLocationVote( bool vote );

        webapp::WebApp*             _p_webApp = nullptr;

        Ui::WidgetLocation*         _p_ui     = nullptr;

        event::ModelEventPtr        _event;

        event::ModelLocationPtr     _location;

        event::ModelLocationVotesPtr _votes;

};

} // namespace event
} // namespace m4e

#endif // WIDGETLOCATION_H
