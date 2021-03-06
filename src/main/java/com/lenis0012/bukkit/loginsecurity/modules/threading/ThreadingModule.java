/*
 * This file is a part of LoginSecurity.
 *
 * Copyright (c) 2017 Lennart ten Wolde
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lenis0012.bukkit.loginsecurity.modules.threading;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.lenis0012.bukkit.loginsecurity.LoginSecurity;
import com.lenis0012.bukkit.loginsecurity.LoginSecurityConfig;
import com.lenis0012.bukkit.loginsecurity.events.AuthModeChangedEvent;
import com.lenis0012.bukkit.loginsecurity.session.AuthMode;
import com.lenis0012.bukkit.loginsecurity.session.AuthService;
import com.lenis0012.bukkit.loginsecurity.session.PlayerSession;
import com.lenis0012.bukkit.loginsecurity.session.action.LoginAction;
import com.lenis0012.bukkit.loginsecurity.util.MetaData;
import com.lenis0012.bukkit.loginsecurity.util.ProfileUtil;
import com.lenis0012.pluginutils.Module;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static com.lenis0012.bukkit.loginsecurity.modules.language.LanguageKeys.*;
import static com.lenis0012.bukkit.loginsecurity.LoginSecurity.translate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ThreadingModule extends Module<LoginSecurity> implements Listener {
    private Cache<UUID, Long> sessionCache;
    private TimeoutTask timeout;
    private MessageTask message;

    public ThreadingModule(LoginSecurity plugin) {
        super(plugin);
    }

    @Override
    public void enable() {
        // threads
        (this.timeout = new TimeoutTask(plugin)).runTaskTimer(plugin, 20L, 20L);
        (this.message = new MessageTask(plugin)).runTaskTimer(plugin, 20L, 20L);
        register(this);

        reload();
    }

    @Override
    public void disable() {
    }

    @Override
    public void reload() {
        final LoginSecurityConfig config = plugin.config();
        final int sessionTimeout = config.getSessionTimeout();

        this.sessionCache = CacheBuilder.newBuilder().expireAfterWrite(sessionTimeout, TimeUnit.SECONDS).build();
        timeout.reload();
        message.reload();
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final PlayerSession session = LoginSecurity.getSessionManager().getPlayerSession(player);
        MetaData.unset(player, "ls_last_message");
        MetaData.unset(player, "ls_login_time");
        if(session.isLoggedIn()) {
            sessionCache.put(ProfileUtil.getUUID(player), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID profileId = ProfileUtil.getUUID(player);
        final Long sessionTime = sessionCache.getIfPresent(profileId);
        MetaData.set(player, "ls_login_time", System.currentTimeMillis());
        if(sessionTime == null) {
            return;
        }

        final long lastLogout = sessionTime;

        // Ip check
        final String ipAddress = player.getAddress().getAddress().toString();
        final PlayerSession session = LoginSecurity.getSessionManager().getPlayerSession(player);
        if(!ipAddress.equals(session.getProfile().getIpAddress())) {
            // Invalid IP
            return;
        }

        // Allow log in once
        final int seconds = (int) ((System.currentTimeMillis() - lastLogout) / 1000L);
        session.performAction(new LoginAction(AuthService.SESSION, plugin));
        player.sendMessage(translate(SESSION_CONTINUE).param("sec", seconds).toString());
    }

    @EventHandler
    public void onAuthModeChanged(AuthModeChangedEvent event) {
        if(event.getCurrentMode() != AuthMode.AUTHENTICATED) {
            // User was logged out.
            MetaData.set(event.getSession().getPlayer(), "ls_login_time", System.currentTimeMillis());
        }
    }
}
