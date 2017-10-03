/**
 * Copyright (c) 2017 by Botorabi. All rights reserved.
 * https://github.com/botorabi/Meet4Eat
 *
 * License: MIT License (MIT), read the LICENSE text in
 *          main directory for more details.
 */


#include <configuration.h>

#include "core.h"
#include "log.h"
#include "utils.h"

#include <settings/appsettings.h>
#include <gui/mainwindow.h>

#include <QDebug>
#include <QApplication>
#include <assert.h>


namespace m4e
{
namespace core
{


//! Log stream buffer for QDebug
static class QtOutputStream: public std::basic_streambuf< char >
{
    public:

                                QtOutputStream() {}

        virtual                 ~QtOutputStream() {}

    protected:

        virtual int_type        overflow( int_type c )
                                {
                                    if( !std::char_traits< char >::eq_int_type( c, std::char_traits< char >::eof() ) )
                                    {
                                        _msg += static_cast< char >( c );
                                        if( c == '\n' )
                                        {
                                            _msg.erase( _msg.length() - 2 );
                                            qDebug() << QString::fromStdString( _msg );
                                            _msg = "";
                                        }
                                    }
                                    return std::char_traits< char >::not_eof( c );
                                }

        std::string             _msg;

} qtOutputStream;

static std::basic_ostream< char > QtLogSteam( &qtOutputStream );


Core::Core()
{
}

Core::~Core()
{
    shutdown();
}

void Core::initialize( int &argc, char* argv[] )
{
    defaultlog.addSink( "qdebug" , QtLogSteam, Log::L_VERBOSE );
    defaultlog.enableSeverityLevelPrinting( false );
    defaultlog.enableTimeStamp( false );
    log_info << "*******************************" << std::endl;
    log_info << "All Rights Reserved by A. Botorabi" << std::endl;
    log_info << M4E_APP_NAME << std::endl;
    log_info << "Version: " << M4E_APP_VERSION << std::endl;
    log_info << "Date: " << getFormatedDateAndTime() << std::endl;
    log_info << "*******************************" << std::endl;
    log_info << std::endl;
    defaultlog.enableSeverityLevelPrinting( true );
    defaultlog.enableTimeStamp( true );
    log_info << "Starting the app" << std::endl;

    _p_app = new QApplication( argc, argv );
    _p_mainWindow = new m4e::gui::MainWindow();
}

void Core::start()
{
    assert( _p_mainWindow && "core was not initialized before" );
    _p_mainWindow->show();
    _p_app->exec();
}

void Core::shutdown()
{
    if ( _p_mainWindow )
    {
        delete _p_mainWindow;
        settings::AppSettings::get()->shutdown();
    }
    _p_mainWindow = nullptr;

    if ( _p_app )
        delete _p_app;
    _p_app = nullptr;
}

} // namespace core
} // namespace m4e