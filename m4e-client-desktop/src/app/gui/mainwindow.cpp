/**
 * Copyright (c) 2017 by Botorabi. All rights reserved.
 * https://github.com/botorabi/Meet4Eat
 *
 * License: MIT License (MIT), read the LICENSE text in
 *          main directory for more details.
 */

#include "mainwindow.h"
#include <core/log.h>
#include <settings/appsettings.h>
#include <settings/dialogsettings.h>
#include <event/widgeteventlist.h>
#include <event/widgetevent.h>
#include <notification/notifyevent.h>
#include <common/basedialog.h>
#include "ui_mainwindow.h"
#include "ui_widgetabout.h"
#include <QLayout>



namespace m4e
{
namespace gui
{

MainWindow::MainWindow() :
 QMainWindow( nullptr ),
 _p_ui( new Ui::MainWindow )
{
    setWindowFlags( Qt::Window | /*Qt::FramelessWindowHint |*/ Qt::CustomizeWindowHint );
    setAttribute( Qt::WA_NoSystemBackground );
    setAttribute( Qt::WA_TranslucentBackground );

    _p_ui->setupUi(this);
    restoreWindowGeometry();

    // prepare the start of webapp, it connects the application to the webapp server
    _p_webApp = new webapp::WebApp( this );
    connect( _p_webApp, SIGNAL( onUserSignedIn( bool, QString ) ), this, SLOT( onUserSignedIn( bool, QString ) ) );
    connect( _p_webApp, SIGNAL( onUserSignedOff( bool ) ), this, SLOT( onUserSignedOff( bool ) ) );
    connect( _p_webApp, SIGNAL( onUserDataReady( m4e::user::ModelUserPtr ) ), this, SLOT( onUserDataReady( m4e::user::ModelUserPtr ) ) );

    // create the chat system
    _p_chatSystem = new chat::ChatSystem( _p_webApp, this );

    _p_initTimer = new QTimer();
    _p_initTimer->setSingleShot( true );
    connect( _p_initTimer, SIGNAL( timeout() ), this, SLOT( onTimerInit() ) );
    _p_initTimer->start( 1000 );

    _p_ui->labelStatus->setText( QApplication::translate( "MainWindow", "Offline" ) );

    clearClientWidget();
}

MainWindow::~MainWindow()
{
    delete _p_ui;
}

void MainWindow::closeEvent( QCloseEvent* p_event )
{
    // hide the window immediately, the dying gasp below takes a few seconds and we don't want a dangling window in that time
    hide();

    // store back the window gemo
    storeWindowGeometry();
    // accept closing
    p_event->setAccepted( true );

    // shutdown the server connection
    _p_webApp->shutdownConnection();
    // process events to give a chance to finish connection shutdown
    for ( unsigned int i = 0; i < 10; i++ )
    {
        qApp->processEvents( QEventLoop::AllEvents, 100 );
        QThread::msleep( 100 );
    }
    log_verbose << TAG << "shutting down the main window" << std::endl;
}

void MainWindow::onTimerInit()
{
    QString remember = settings::AppSettings::get()->readSettingsValue( M4E_SETTINGS_CAT_USER, M4E_SETTINGS_KEY_USER_PW_REM, "yes" );
    if ( remember == "yes" )
    {
        _p_webApp->establishConnection();
    }
}

void MainWindow::storeWindowGeometry()
{
    QSettings* p_settings = settings::AppSettings::get()->getSettings();
    QByteArray geom = saveGeometry();
    p_settings->setValue( M4E_SETTINGS_KEY_WIN_GEOM, geom );
}

void MainWindow::restoreWindowGeometry()
{
    QSettings* p_settings = settings::AppSettings::get()->getSettings();
    QByteArray geom =  p_settings->value( M4E_SETTINGS_KEY_WIN_GEOM ).toByteArray();
    restoreGeometry( geom );
}

void MainWindow::onBtnCloseClicked()
{
    // first exe the closeEvent handler
    QCloseEvent event;
    closeEvent( &event );

    QApplication::quit();
}

void MainWindow::mouseDoubleClickEvent( QMouseEvent* p_event )
{
    // drag the window only by the means of head-bar
    if ( !_p_ui->widgetHead->geometry().contains( p_event->pos() ) )
        return;

    onBtnMaximizeClicked();
}

void MainWindow::mousePressEvent( QMouseEvent* p_event )
{
    // drag the window only by the means of head-bar
    if ( !_p_ui->widgetHead->geometry().contains( p_event->pos() ) )
        return;

    _draggingPos = p_event->pos();
    _dragging = true;
}

void MainWindow::mouseReleaseEvent( QMouseEvent* /*p_event*/ )
{
    _dragging = false;
}

void MainWindow::mouseMoveEvent( QMouseEvent* p_event )
{
    if ( _dragging )
    {
        move( p_event->globalPos() - _draggingPos );
    }
}

void MainWindow::onBtnMinimizeClicked()
{
    setWindowState( Qt::WindowMinimized );
}

void MainWindow::onBtnMaximizeClicked()
{
    if ( windowState() & Qt::WindowMaximized )
    {
        setWindowState( windowState() & ~Qt::WindowMaximized );
    }
    else
    {
        setWindowState( Qt::WindowMaximized );
    }
}

void MainWindow::onBtnEventsClicked()
{
    clearClientWidget();
    clearMyEventsWidget();
    createWidgetMyEvents();
}

void MainWindow::onBtnSettingsClicked()
{
    settings::DialogSettings* dlg = new settings::DialogSettings( _p_webApp, this );
    dlg->exec();
    delete dlg;
}

void MainWindow::onBtnAboutClicked()
{
    Ui::WidgetAbout about;
    common::BaseDialog* p_dlg = new common::BaseDialog( this );
    p_dlg->decorate( about );

    QString text = about.labelText->text();
    text.replace( "@APP_NAME@", M4E_APP_NAME );
    text.replace( "@APP_VERSION@", M4E_APP_VERSION );
    text.replace( "@COPYRIGHT@", M4E_APP_COPYRIGHT );
    text.replace( "@URL@", M4E_APP_URL );
    about.labelText->setText( text );

    p_dlg->setTitle( "About " + QString( M4E_APP_NAME ) );
    QString btnok( "Ok" );
    p_dlg->setupButtons( &btnok, nullptr, nullptr );
    p_dlg->setResizable( false );
    p_dlg->exec();
    delete p_dlg;
}

void MainWindow::onEventSelection( QString id )
{
    clearClientWidget();
    createWidgetEvent( id );
}

void MainWindow::onUserDataReady( user::ModelUserPtr user )
{
    QString text;
    if ( user.valid() )
    {
        text = QApplication::translate( "MainWindow", "User: " ) + user->getName();
    }
    else
    {
        text = QApplication::translate( "MainWindow", "No Connection!" );
    }
    _p_ui->labelStatus->setText( text );

    connect( _p_webApp->getNotifications(), SIGNAL( onEventLocationChanged( m4e::notify::Notifications::ChangeType, QString, QString ) ), this,
                                            SLOT( onEventLocationChanged( m4e::notify::Notifications::ChangeType, QString, QString ) ) );

    connect( _p_webApp->getEvents(), SIGNAL( onResponseGetEvents( bool, QList< m4e::event::ModelEventPtr > ) ), this,
                                     SLOT( onResponseGetEvents( bool, QList< m4e::event::ModelEventPtr > ) ) );

    _p_webApp->getEvents()->requestGetEvents();
}

void MainWindow::onUserSignedIn( bool success, QString userId )
{
    if ( success )
    {
        log_verbose << TAG << "user was successfully signed in: " << userId << std::endl;
        // create the chat system
        _p_chatSystem = new chat::ChatSystem( _p_webApp, this );
        addLogText( "User has successfully signed in" );
    }
    else
    {
        log_verbose << TAG << "user could not sign in: " << userId << std::endl;
        _p_ui->labelStatus->setText( QApplication::translate( "MainWindow", "Offline" ) );
        addLogText( "User failed to signed in!" );
    }
}

void MainWindow::onUserSignedOff( bool success )
{
    _p_ui->labelStatus->setText( QApplication::translate( "MainWindow", "Offline" ) );

    delete _p_chatSystem;
    _p_chatSystem = nullptr;

    if ( success )
    {
        clearMyEventsWidget();
        createWidgetMyEvents();
    }

    addLogText( "User has signed off" );
}

void MainWindow::onResponseGetEvents( bool /*success*/, QList< event::ModelEventPtr > /*events*/ )
{
    clearMyEventsWidget();
    createWidgetMyEvents();
}

void MainWindow::onEventLocationChanged( notify::Notifications::ChangeType changeType, QString eventId, QString locationId )
{
    log_verbose << TAG << "notification: event location was changed: " << eventId << "/" << locationId << std::endl;

    addLogText( "Event location settings have changed" );

    if ( changeType == notify::Notifications::Removed )
    {
        //! TODO let the user know about the update
    }
    else
    {
        //! TODO let the user know about the update
    }
}

void MainWindow::addLogText( const QString& text )
{
    QDateTime timestamp = QDateTime::currentDateTime();
    QString ts = "[" + timestamp.toString( "yyyy-M-dd HH:mm:ss" ) + "]";
    QString logmsg = "<p>";
    logmsg += "<span style='color: gray;'>" + ts + "</span>";
    logmsg += " <span style='color: white;'>" + text + "</span>";
    logmsg += "</p>";
    _p_ui->textNotify->appendHtml( logmsg );
}

void MainWindow::clearClientWidget()
{
    QLayoutItem* p_item;
    QLayout* p_layout = _p_ui->widgetClientArea->layout();
    while ( ( p_layout->count() > 0 ) && ( nullptr != ( p_item = _p_ui->widgetClientArea->layout()->takeAt( 0 ) ) ) )
    {
        p_item->widget()->deleteLater();
        delete p_item;
    }
}

void MainWindow::clearMyEventsWidget()
{
    QLayoutItem* p_item;
    QLayout* p_layout = _p_ui->widgetSubMenu->layout();
    while ( ( p_layout->count() > 0 ) && ( nullptr != ( p_item = _p_ui->widgetSubMenu->layout()->takeAt( 0 ) ) ) )
    {
        p_item->widget()->deleteLater();
        delete p_item;
    }
}

void MainWindow::createWidgetMyEvents()
{
    clearClientWidget();

    event::WidgetEventList* p_widget = new event::WidgetEventList( _p_webApp, this );
    _p_ui->widgetSubMenu->layout()->addWidget( p_widget );
    connect( p_widget, SIGNAL( onEventSelection( QString /*id*/ ) ), this, SLOT( onEventSelection( QString /*id*/ ) ) );
    // auto-select the first event
    p_widget->selectFirstEvent();
}

void MainWindow::createWidgetEvent( const QString& eventId )
{
    event::WidgetEvent* p_widget = new event::WidgetEvent( _p_webApp, _p_ui->widgetClientArea );
    p_widget->setEvent( eventId );

    if ( _p_chatSystem )
        p_widget->setChatSystem( _p_chatSystem );

    _p_ui->widgetClientArea->layout()->addWidget( p_widget );
}

} // namespace gui
} // namespace m4e
