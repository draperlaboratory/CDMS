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
package com.draper.services.c2;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.draper.poller.PollInterface;
import com.draper.utilities.UDPMulticast;
import com.google.gson.Gson;

// ---------------------------------------------------------------------------------------------------------------
//
//
// ---------------------------------------------------------------------------------------------------------------
public final class C2Services implements PollInterface
{
    private static Logger           logger   = Logger.getLogger(C2Services.class.getName());

    private static final C2Services instance = new C2Services();
    private ArrayList<C2Msg>        messages = new ArrayList<C2Msg>();
    private int                     seconds  = 0;

    /**************************************************************************************** I
     * Instance pattern for
     * singleton 
     */
    static public C2Services Instance()
    {
        return instance;
    }

    /********************************************************************************************** 
     * @return 
     */
    public synchronized void BroadcastMessage(C2Msg msg)
    {
        try
        {
            if (false == messages.contains(msg))
            {
                messages.add(msg);
            }

        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Exception: " + e.getMessage(), e);
        }
        catch (Error e)
        {
            logger.log(Level.SEVERE, "Exception: " + e.getMessage(), e);
        }
    }

    /****************************************************************************************
     * 
     */
    int GetClockTicks()
    {
        return seconds;
    }

    /****************************************************************************************
     * 
     */
    @Override
    public synchronized void Execute(int counter, Timestamp ts) throws Exception
    {
        try
        {
            seconds++;

            for (C2Msg msg : messages)
            {
                String json = new Gson().toJson(msg);

                UDPMulticast.Broadcast(json);
            }

            messages.clear();

        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Exception: " + e.getMessage(), e);
        }
    }

    /****************************************************************************************
     * 
     */
    @Override
    public void Stop() throws Exception
    {
    }
}
