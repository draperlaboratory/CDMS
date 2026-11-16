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
package com.draper.utilities;

import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.nio.file.StandardOpenOption;

public class Logging
{
    private static final int         MAX_FILE_SIZE = 10 * 1000 * 1000; // 10MB
    private static RollingLogHandler fileHandler;
    private static Logger            logger        = Logger.getLogger("com.draper");

    /** Initializes logging by ensuring the correct log file exists, and an output stream is available to write to. Called
     * on startup, and when logging turns over to a new file. */
    public static void initialize(String fileBase, boolean useStdOut, String minLevel)
    {
        try
        {
            Level level = parseLogLevel(minLevel);

            // Set up console logging
            logger.setUseParentHandlers(useStdOut);

            // Set up file logging
            logger.removeHandler(fileHandler);
            logger.setLevel(level);
            fileHandler = new RollingLogHandler(fileBase, MAX_FILE_SIZE);
            fileHandler.setFormatter(new LogFormatter());
            fileHandler.setLevel(level);

            logger.addHandler(fileHandler);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static boolean getStdoutEcho()
    {
        return logger.getUseParentHandlers();
    }

    /**
     * Returns a java.util.logging.Level from the provided CDMS log level. Returns INFO when logLevel is null. Throws an error on an invalid log level string.
     */
    public static Map<String, Level> levels = Map.of("ERROR", Level.SEVERE, "WARN", Level.WARNING, "INFO", Level.INFO, "DEBUG", Level.CONFIG);

    public static void close()
    {
        fileHandler.flush();
        fileHandler.close();
    }

    public static Level parseLogLevel(String logLevel) throws Exception
    {
        if (logLevel == null)
        {
            return Level.INFO;
        }
        Level level = levels.get(logLevel);
        if (level == null)
        {
            Exception e = new Exception("Invalid log level");
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
        return level;
    }

    public static Integer getCurrentLogIndex()
    {
        return fileHandler.getCurrentLogIndex();
    }

    public static Path getLogFile(int index)
    {
        return fileHandler.getLogFile(index);
    }
}

/**
 * This class is intended to be similar to FileHandler, but support infinite log files, without a maximum number of files.
 */
class RollingLogHandler extends Handler
{

    private long         maxFileSize;
    private long         writtenBytes   = 0;
    private OutputStream currentOutputStream;
    private List<Path>   logFileHistory = new ArrayList<>();
    private String       absoluteFileNamePrefix;
    private String       lastLogEntryDateString;

    public RollingLogHandler(String absoluteFileNamePrefix, long maxFileSize) throws IOException
    {
        this.maxFileSize = maxFileSize;
        this.absoluteFileNamePrefix = absoluteFileNamePrefix;
        this.initializeFiles();
    }

    private void initializeFiles() throws IOException
    {
        Path prefix = Paths.get(absoluteFileNamePrefix);
        Path directory = prefix.getParent();
        String fileName = prefix.getFileName().toString();
        Files.createDirectories(directory);

        // Collect all existing log files
        this.logFileHistory = new ArrayList<>();
        for (Path logFile : Files.newDirectoryStream(directory))
        {
            if (logFile.getFileName().toString().endsWith(".txt"))
            {
                this.logFileHistory.add(logFile);
            }
        }
        this.logFileHistory.sort((path1, path2) -> path1.toString().compareToIgnoreCase(path2.toString()));

        // Find most recent log file, and see if it should be appended to
        Path mostRecentLogFile = getLogFile(getCurrentLogIndex());
        String currentDate = UiUtil.GPXDATE_FORMAT.format(new Date());
        String todaysPrefix = fileName + "_" + currentDate;

        if (mostRecentLogFile != null)
        {
            this.writtenBytes = Files.size(mostRecentLogFile);
            this.currentOutputStream = Files.newOutputStream(mostRecentLogFile, StandardOpenOption.APPEND);
            if (mostRecentLogFile.getFileName().toString().startsWith(todaysPrefix))
            {
                this.lastLogEntryDateString = currentDate;
            }
        }
    }

    private void rotate(String formattedDate) throws IOException
    {
        if (this.currentOutputStream != null)
        {
            this.currentOutputStream.flush();
            this.currentOutputStream.close();
        }

        String todaysPrefix = this.absoluteFileNamePrefix + "_" + formattedDate;

        // If we need to rotate for the new date, than use _0 suffix. Otherwise, increment the suffix on the most recent log
        // file.
        if (this.lastLogEntryDateString == null || !this.lastLogEntryDateString.equals(formattedDate))
        {
            todaysPrefix += "_0.txt";
            lastLogEntryDateString = formattedDate;
        }
        else
        {
            String mostRecentLogFile = getLogFile(getCurrentLogIndex()).toString();
            int underscoreIndex = mostRecentLogFile.lastIndexOf("_");
            int dotIndex = mostRecentLogFile.lastIndexOf(".");
            if (underscoreIndex == -1 || dotIndex == -1 || dotIndex < underscoreIndex)
            {
                throw new IOException("Error getting the index of last log file.");
            }
            String indexStr = mostRecentLogFile.substring(underscoreIndex + 1, dotIndex);
            int nextIndex = Integer.parseInt(indexStr) + 1;
            todaysPrefix += "_" + nextIndex + ".txt";
        }
        Path newLogFilePath = Paths.get(todaysPrefix);
        this.logFileHistory.add(newLogFilePath);

        this.currentOutputStream = Files.newOutputStream(newLogFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        this.writtenBytes = 0;
        this.lastLogEntryDateString = formattedDate;
    }

    @Override
    public void publish(LogRecord record)
    {
        if (this.isLoggable(record))
        {
            byte[] bytes = {};
            String formattedDate = UiUtil.GPXDATE_FORMAT.format(new Date());
            try
            {
                Formatter formatter = this.getFormatter();
                String formatted = formatter.format(record) + "\n";
                bytes = formatted.getBytes();
            }
            catch (Exception e)
            {
                this.reportError(null, e, ErrorManager.FORMAT_FAILURE);
            }

            try
            {
                if (writtenBytes + bytes.length > maxFileSize || this.lastLogEntryDateString == null || !formattedDate.equals(this.lastLogEntryDateString))
                {
                    rotate(formattedDate);
                }
                this.currentOutputStream.write(bytes);
                this.writtenBytes += bytes.length;
            }
            catch (Exception e)
            {
                this.reportError(null, e, ErrorManager.WRITE_FAILURE);
            }
        }
    }

    @Override
    public void close() throws SecurityException
    {
        try
        {
            this.currentOutputStream.close();
        }
        catch (Exception e)
        {
            this.reportError(null, e, ErrorManager.CLOSE_FAILURE);
        }
    }

    @Override
    public void flush()
    {
        try
        {
            this.currentOutputStream.flush();
        }
        catch (Exception e)
        {
            this.reportError(null, e, ErrorManager.FLUSH_FAILURE);
        }
    }

    public Integer getCurrentLogIndex()
    {
        int index = this.logFileHistory.size();
        if (index == 0)
        {
            return -1;
        }

        return index - 1;
    }

    public Path getLogFile(int index)
    {
        if (index < 0 || index >= this.logFileHistory.size())
        {
            return null;
        }
        return this.logFileHistory.get(index);
    }
}
