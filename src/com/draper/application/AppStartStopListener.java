/* =============================================================================
 * Vader Modular Fuzzer (VMF)
 * Copyright (c) 2021-2026 The Charles Stark Draper Laboratory, Inc.
 * <vmf@draper.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 (only) as 
 * published by the Free Software Foundation.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *  
 * @license GPL-2.0-only <https://spdx.org/licenses/GPL-2.0-only.html>
 * ===========================================================================*/
package com.draper.application;

import java.io.File;
import java.io.Reader;
import java.nio.file.Path;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.draper.poller.PollManager;
import com.draper.services.c2.C2Services;
import com.draper.services.corpus.CorpusServices;
import com.draper.services.database.DatabaseService;
import com.draper.utilities.Logging;
import com.draper.utilities.UiUtil;
import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;
import com.ibatis.sqlmap.client.SqlMapClient;

public class AppStartStopListener implements ServletContextListener
{
    static Logger logger = Logger.getLogger(AppStartStopListener.class.getName());

    /** Called when the web application starts up. */
    public void contextInitialized(ServletContextEvent sce)
    {
        try
        {
            // ---------------------------------------------------------------------------------------
            // Get the Base Path of the WebService

            AppConfig.webservicePath = new File(sce.getServletContext().getRealPath("/")).getParentFile().getAbsolutePath();

            // ---------------------------------------------------------------------------------------
            // Get the Directory where the WebService will store its files

            AppConfig.storagePath = sce.getServletContext().getInitParameter("storagePath");

            if (true == AppConfig.storagePath.trim().isEmpty())
            {
                AppConfig.storagePath = AppConfig.webservicePath;
            }

            // ---------------------------------------------------------------------------------------
            // Initialize the SQL map

            Reader reader = Resources.getResourceAsReader(AppConfig.sqlMapConfig);
            SqlMapClient sqlMap = SqlMapClientBuilder.buildSqlMapClient(reader);

            DatabaseService.Instance().Init(sqlMap);

            // ---------------------------------------------------------------------------------------
            // Initialize the Logger.

            String stdoutEchoProp = UiUtil.notNull(DatabaseService.Instance().getProperty("use.stdout"));
            Path logfileBaseName = Path.of(AppConfig.storagePath, "logs", sce.getServletContext().getInitParameter("logFileBaseName"));
            String minLogLevel = sce.getServletContext().getInitParameter("logLevel");

            Logging.initialize(logfileBaseName.toString(), Boolean.parseBoolean(stdoutEchoProp), minLogLevel);
            Path currentLogFile = Logging.getLogFile(Logging.getCurrentLogIndex());

            // ---------------------------------------------------------------------------------------
            // Initialize Corpus Service that will handle all the cluster and test case storage

            CorpusServices.Instance().Initialize(AppConfig.storagePath);

            // ---------------------------------------------------------------------------------------
            // Install Pollers

            PollManager.Instance().Init(1, 5); // One Thread, Five Second Delay Startup

            PollManager.Instance().InstallPoller(C2Services.Instance(), 1, TimeUnit.SECONDS);

            // ---------------------------------------------------------------------------------------
            // Web Server and WebService Identification Information

            AppConfig.serverIdentificationInfo = sce.getServletContext().getServerInfo();
            AppConfig.webServiceIdentificationInfo = sce.getServletContext().getServletContextName() + " Release " + AppConfig.softwareVersion;

            // Print the information we will use to initialize the system
            logger.info("Server Id  : " + AppConfig.serverIdentificationInfo);
            logger.info("WebService : " + AppConfig.webServiceIdentificationInfo);
            logger.info("Log        : " + (currentLogFile != null ? currentLogFile.toString() : "null"));
            logger.info("Storage    : " + AppConfig.storagePath);
            logger.info("WebRoot    : " + AppConfig.webservicePath);
            logger.info("Use Stdout : " + Boolean.parseBoolean(stdoutEchoProp));
            logger.info("WebService Initialized.");
        }
        catch (Exception e)
        {
            logger.severe("WebService Failed to Initialize: " + AppConfig.webServiceIdentificationInfo);
            e.printStackTrace();
        }
    }

    /** Called when the web application is stopped or un-deployed. */
    public void contextDestroyed(ServletContextEvent sce)
    {
        try
        {
            logger.info("WebService Shutting Down");

            PollManager.Instance().Stop();

            // DeRegister JDBC driver
            Enumeration<Driver> drivers = DriverManager.getDrivers();

            while (drivers.hasMoreElements())
            {
                Driver driver = drivers.nextElement();

                try
                {
                    DriverManager.deregisterDriver(driver);
                    logger.info("Deregistering jdbc driver: " + driver);
                }
                catch (Exception e)
                {
                    logger.info("Error deregistering driver: " + driver);
                }
            }

            // Complete the Logging
            logger.info("WebService Stopped");
            Logging.close();
        }
        catch (Exception e)
        {
            logger.severe(e.toString());
        }
    }
}
