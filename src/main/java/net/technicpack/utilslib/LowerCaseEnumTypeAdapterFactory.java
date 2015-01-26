/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.utilslib;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LowerCaseEnumTypeAdapterFactory implements TypeAdapterFactory {

    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class rawType = type.getRawType();
        if (!rawType.isEnum()) {
            return null;
        }

        final Map<String, T> lowercaseToConstant = new HashMap<String, T>();
        for (Object constant : rawType.getEnumConstants()) {
            lowercaseToConstant.put(toLowercase(constant), (T) constant);
        }

        return new TypeAdapter<T>() {
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null)
                    out.nullValue();
                else
                    out.value(LowerCaseEnumTypeAdapterFactory.this.toLowercase(value));
            }

            public T read(JsonReader reader) throws IOException {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    return null;
                }
                return lowercaseToConstant.get(reader.nextString());
            }
        };
    }

    private String toLowercase(Object o) {
        return o.toString().toLowerCase(Locale.US);
    }
}