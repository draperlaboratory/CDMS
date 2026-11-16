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

import java.lang.reflect.Type;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class LogFormatter extends Formatter
{
    static Gson gson = new GsonBuilder().registerTypeAdapter(Exception.class, new ExceptionSerializer()).registerTypeAdapter(Date.class, new DateSerializer()).create();

    public String format(LogRecord record)
    {
        String loggerName = record.getLoggerName();
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("time", Date.from(record.getInstant()));
        logEntry.put("level", record.getLevel().getName());
        logEntry.put("source", new JsonPrimitive(loggerName.substring(loggerName.lastIndexOf(".") + 1)));
        logEntry.put("message", formatMessage(record));

        return gson.toJson(logEntry);
    }

    class ClassNameSerializer implements JsonSerializer<Object>
    {
        @Override
        public JsonElement serialize(Object obj, Type type, JsonSerializationContext ctx)
        {
            if (obj == null)
            {
                return null;
            }
            String classFullName = obj.getClass().getName();
            return new JsonPrimitive(classFullName.substring(classFullName.lastIndexOf(".") + 1));
        }
    }

    static class DateSerializer implements JsonSerializer<Date>
    {
        @Override
        public JsonElement serialize(Date date, Type type, JsonSerializationContext ctx)
        {
            return date == null ? null : new JsonPrimitive(date.getTime());
        }
    }

    static class ExceptionSerializer implements JsonSerializer<Exception>
    {
        @Override
        public JsonElement serialize(Exception e, Type type, JsonSerializationContext ctx)
        {
            StringJoiner joiner = new StringJoiner("\n");
            for (StackTraceElement ste : e.getStackTrace())
            {
                joiner.add("\t\t" + ste.toString());
            }
            return new JsonPrimitive(joiner.toString());
        }
    }
}