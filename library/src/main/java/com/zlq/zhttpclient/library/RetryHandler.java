/*
    Android Asynchronous Http Client
    Copyright (c) 2011 James Smith <james@loopj.com>
    http://loopj.com

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

/*
    Some of the retry logic in this class is heavily borrowed from the
    fantastic droid-fu project: https://github.com/donnfelker/droid-fu
*/

package com.zlq.zhttpclient.library;

import android.os.SystemClock;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;

import javax.net.ssl.SSLException;

/**
 * Created by zhanglq on 15/5/31.
 */
class RetryHandler {
    private final static HashSet<Class<?>> exceptionWhitelist = new HashSet<Class<?>>();
    private final static HashSet<Class<?>> exceptionBlacklist = new HashSet<Class<?>>();

    static {
        // retry-this, since it may happens as part of a Wi-Fi to 3G failover
        exceptionWhitelist.add(UnknownHostException.class);
        // retry-this, since it may happens as part of a Wi-Fi to 3G failover
        exceptionWhitelist.add(SocketException.class);

        // never retry timeouts
        exceptionBlacklist.add(InterruptedIOException.class);
        // never retry SSL handshake failures
        exceptionBlacklist.add(SSLException.class);
    }

    private final int maxRetries;
    private final int retrySleepTimeMS;

    public RetryHandler(int maxRetries, int retrySleepTimeMS) {
        this.maxRetries = maxRetries;
        this.retrySleepTimeMS = retrySleepTimeMS;
    }

    public boolean retryRequest(IOException exception, int executionCount) {
        boolean retry = false;

        if (executionCount > maxRetries) {
            // Do not retry if over max retry count
            retry = false;
        } else if (isInList(exceptionWhitelist, exception)) {
            // immediately retry if error is whitelisted
            retry = true;
        } else if (isInList(exceptionBlacklist, exception)) {
            // immediately cancel retry if the error is blacklisted
            retry = false;
        }

        if (retry) {
            SystemClock.sleep(retrySleepTimeMS);
        } else {
            exception.printStackTrace();
        }

        return retry;
    }

    static void addClassToWhitelist(Class<?> cls) {
        exceptionWhitelist.add(cls);
    }

    static void addClassToBlacklist(Class<?> cls) {
        exceptionBlacklist.add(cls);
    }

    protected boolean isInList(HashSet<Class<?>> list, Throwable error) {
        for (Class<?> aList : list) {
            if (aList.isInstance(error)) {
                return true;
            }
        }
        return false;
    }
}
