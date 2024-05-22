/*
 * This file is part of Technic Minecraft Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Minecraft Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Minecraft Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Minecraft Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.minecraftcore.mojang.auth.request;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class AuthRequest {
    private final Agent agent;
    private final String username;
    private final String password;
    private final String clientToken;
    private final boolean requestUser = true;

    public AuthRequest(String username, String password, String clientToken) {
        this.agent = new Agent();
        this.username = username;
        this.password = password;
        this.clientToken = clientToken;
    }

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    public static class Agent {
        private final String name = "Minecraft";
        private final int version = 1;
    }
}
