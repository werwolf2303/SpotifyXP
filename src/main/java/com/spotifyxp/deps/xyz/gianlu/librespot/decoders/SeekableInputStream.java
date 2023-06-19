/*
 * Copyright 2021 devgianlu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spotifyxp.deps.xyz.gianlu.librespot.decoders;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author devgianlu
 */
@SuppressWarnings("NullableProblems")
public abstract class SeekableInputStream extends InputStream {
    public abstract int size();

    public abstract int position();

    public abstract void seek(int seekZero) throws IOException;

    public abstract long skip(long skip) throws IOException;

    public abstract int read(byte[] buffer, int index, int length) throws IOException;

    public abstract void close();

    public abstract int decodedLength();
}
