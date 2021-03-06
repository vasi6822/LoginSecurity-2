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

package com.lenis0012.bukkit.loginsecurity.util;

import com.lenis0012.bukkit.loginsecurity.LoginSecurity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class MetaData {

    public static void set(Player player, String key, Object value) {
        player.setMetadata(key, new FixedMetadataValue(LoginSecurity.getInstance(), value));
    }

    public static boolean has(Player player, String key) {
        return get(player, key, Object.class) != null;
    }

    public static <T> T get(Player player, String key, T def) {
        Object value = get(player, key, Object.class);
        return value == null ? def : (T) value;
    }

    public static <T> T get(Player player, String key, Class<T> type) {
        if(!player.hasMetadata(key)) {
            return null;
        }

        for(MetadataValue value : player.getMetadata(key)) {
            if(value.getOwningPlugin().equals(LoginSecurity.getInstance())) {
                return type.cast(value.value());
            }
        }

        return null;
    }

    public static void unset(Player player, String key) {
        player.removeMetadata(key, LoginSecurity.getInstance());
    }
}
