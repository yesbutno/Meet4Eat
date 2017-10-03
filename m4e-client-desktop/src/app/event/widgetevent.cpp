/**
 * Copyright (c) 2017 by Botorabi. All rights reserved.
 * https://github.com/botorabi/Meet4Eat
 *
 * License: MIT License (MIT), read the LICENSE text in
 *          main directory for more details.
 */

#include <configuration.h>
#include "widgetevent.h"
#include <core/log.h>
#include <QListWidget>
#include <QPushButton>
#include <QHBoxLayout>
#include <QVBoxLayout>
#include <ui_widgetevent.h>
#include "widgeteventitem.h"
#include "widgetlocation.h"


namespace m4e
{
namespace event
{

WidgetEvent::WidgetEvent( webapp::WebApp* p_webApp, QWidget* p_parent ) :
 QWidget( p_parent ),
 _p_webApp( p_webApp )
{
    setupUI();
}

WidgetEvent::~WidgetEvent()
{
    if ( _p_ui )
        delete _p_ui;
}

void WidgetEvent::setEvent( const QString& id )
{
    QList< event::ModelEventPtr > events = _p_webApp->getEvents()->getUserEvents();
    event::ModelEventPtr event;
    for ( event::ModelEventPtr ev: events )
    {
        if ( ev->getId() == id )
        {
            event = ev;
            break;
        }
    }

    if ( !event.valid() )
    {
        log_error << TAG << "could not find the event with id: " << id.toStdString() << std::endl;
    }
    else
    {
        _locations.clear();
        _eventId = id;
        _p_ui->widgetChat->setChannel( _eventId );
        if ( event->getLocations().size() > 0 )
        {
            for ( auto location: event->getLocations() )
            {
                addLocation( location );
            }
        }
        else
        {
            setupNoLocationWidget();
        }
        setupWidgetHead( event );
    }
}

void WidgetEvent::setupUI()
{
    _p_ui = new Ui::WidgetEvent;
    _p_ui->setupUi( this );
    _p_ui->widgetChat->setWebApp( _p_webApp );

    _p_clientArea = _p_ui->listWidget;

    _p_clientArea->setUniformItemSizes( true );
    _p_clientArea->setSizePolicy( QSizePolicy::Expanding, QSizePolicy::Expanding );
    _p_clientArea->setAutoScroll( true );
    _p_clientArea->setViewMode( QListView::IconMode );
    _p_clientArea->setWrapping( true );
    _p_clientArea->setSpacing( 10 );
}

void WidgetEvent::setupWidgetHead( event::ModelEventPtr event )
{
    _p_ui->labelInfoHead->setText( event->getName() );
    QString info;
    if ( event->isRepeated() )
    {
        QTime time = event->getRepeatDayTime();
        unsigned int days = event->getRepeatWeekDays();
        QString weekdays;
        weekdays += ( days & event::ModelEvent::WeekDayMonday )    != 0 ? " Mon" : "";
        weekdays += ( days & event::ModelEvent::WeekDayTuesday )   != 0 ? " Tue" : "";
        weekdays += ( days & event::ModelEvent::WeekDayWednesday ) != 0 ? " Wed" : "";
        weekdays += ( days & event::ModelEvent::WeekDayThursday )  != 0 ? " Tur" : "";
        weekdays += ( days & event::ModelEvent::WeekDayFriday )    != 0 ? " Fri" : "";
        weekdays += ( days & event::ModelEvent::WeekDaySaturday )  != 0 ? " Sat" : "";
        weekdays += ( days & event::ModelEvent::WeekDaySunday )    != 0 ? " Sun" : "";
        info += "Repeated Event";
        info += "\n * Week Days:" + weekdays;
        info += "\n * At " + QString( "%1" ).arg( time.hour(), 2, 10, QChar( '0' ) ) + ":" + QString( "%1" ).arg( time.minute(), 2, 10, QChar( '0' ) );
    }
    else
    {
        info += "Event date: " + event->getStartDate().toString();
    }
    _p_ui->labelInfoBody->setText( info );
}

void WidgetEvent::setupNoLocationWidget()
{
    //! TODO we need a good looking widget here!
    _p_clientArea->addItem( "Event has no location!" );
}

void WidgetEvent::addLocation( event::ModelLocationPtr location )
{
    WidgetLocation* p_widget = new WidgetLocation( _p_webApp, _p_clientArea );
    p_widget->setupUI( location );

    QListWidgetItem* p_item = new QListWidgetItem( _p_clientArea );
    p_item->setSizeHint( p_widget->size() );

    _p_clientArea->setItemWidget( p_item, p_widget );
    p_item->setData( Qt::UserRole, location->getId() );

    //! NOTE this is really needed after every item insertion, otherwise the items get draggable
    _p_clientArea->setDragDropMode( QListWidget::NoDragDrop );

    _locations.insert( location->getId(), location->getName() );
}

void WidgetEvent::onButtonBuzzClicked()
{
    log_verbose << TAG << "poke event members..." << std::endl;
}

} // namespace event
} // namespace m4e