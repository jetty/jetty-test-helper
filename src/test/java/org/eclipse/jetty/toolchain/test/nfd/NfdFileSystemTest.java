//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.toolchain.test.nfd;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;

import org.eclipse.jetty.toolchain.test.jupiter.WorkDir;
import org.eclipse.jetty.toolchain.test.jupiter.WorkDirExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(WorkDirExtension.class)
public class NfdFileSystemTest
{
    // "café.txt": the "é" is a single precomposed code point (U+00E9), i.e. Form C (NFC).
    private static final String NFC_NAME = "café.txt";
    // The same name decomposed, Form D (NFD): "e" + combining acute accent (U+0301).
    private static final String NFD_NAME = "café.txt";
    // UTF-8 percent-encoding of the decomposed run ("e" is ASCII, U+0301 is CC 81).
    private static final String NFD_URI_ENCODED = "e%CC%81.txt";
    // UTF-8 percent-encoding of the precomposed "é" (C3 A9).
    private static final String NFC_URI_ENCODED = "caf%C3%A9.txt";

    private NfdFileSystem newFileSystem(NfdFileSystem.Mode mode)
    {
        NfdFileSystemProvider provider = new NfdFileSystemProvider();
        return new NfdFileSystem(provider, FileSystems.getDefault(), mode);
    }

    private Path createNfcFile(WorkDir workDir) throws IOException
    {
        Path real = workDir.getEmptyPathDir().resolve(NFC_NAME);
        Files.createFile(real);
        return real;
    }

    @Test
    public void testProviderScheme()
    {
        assertThat(new NfdFileSystemProvider().getScheme(), is("nfd"));
    }

    @Test
    public void testFileSystemIsOpenAndCloseIsNoOp()
    {
        NfdFileSystem fs = newFileSystem(NfdFileSystem.Mode.DECODE_TO_NFC);
        assertTrue(fs.isOpen());
        fs.close();
        // close() must never close the shared default filesystem.
        assertTrue(fs.isOpen());
        assertTrue(FileSystems.getDefault().isOpen());
    }

    @Test
    public void testGetPathReturnsNfdPathBackedByRealFile(WorkDir workDir) throws IOException
    {
        Path real = createNfcFile(workDir);
        NfdFileSystem fs = newFileSystem(NfdFileSystem.Mode.DECODE_TO_NFC);

        Path wrapped = fs.getPath(real.toString());
        assertThat(wrapped, instanceOf(NfdPath.class));
        assertThat(wrapped.getFileSystem(), is(fs));

        // Operations forward to the default provider after unwrapping, so the real file is seen.
        assertTrue(Files.exists(wrapped));
        assertTrue(Files.isRegularFile(wrapped));
        // The plain String/toString() form is unchanged (still the on-disk NFC name).
        assertThat(wrapped.toString(), is(real.toString()));
    }

    @Test
    public void testDecodeToNfcMode(WorkDir workDir) throws IOException
    {
        Path real = createNfcFile(workDir);
        NfdFileSystem fs = newFileSystem(NfdFileSystem.Mode.DECODE_TO_NFC);

        Path resolved = fs.getPath(real.toString()).toRealPath();

        // JDK 19 and earlier: toString() stays Form C (matches the on-disk/request name) ...
        assertThat(resolved.getFileName().toString(), is(NFC_NAME));
        assertThat(resolved.getFileName().toString(), is(Normalizer.normalize(resolved.getFileName().toString(), Normalizer.Form.NFC)));
        // ... but toUri() reports the raw NFD bytes (Form D), not the precomposed Form C encoding.
        String uri = resolved.toUri().toString();
        assertThat(uri, containsString(NFD_URI_ENCODED));
        assertThat(uri, not(containsString(NFC_URI_ENCODED)));
    }

    @Test
    public void testRawNfdMode(WorkDir workDir) throws IOException
    {
        Path real = createNfcFile(workDir);
        NfdFileSystem fs = newFileSystem(NfdFileSystem.Mode.RAW_NFD);

        Path resolved = fs.getPath(real.toString()).toRealPath();

        // JDK 20 and later: both toString() and toUri() are Form D (the name no longer matches Form C).
        assertThat(resolved.getFileName().toString(), is(NFD_NAME));
        assertThat(resolved.getFileName().toString(), not(is(NFC_NAME)));
        assertThat(resolved.toUri().toString(), containsString(NFD_URI_ENCODED));
    }

    @Test
    public void testEqualsAndHashCodeTrackBackingPath(WorkDir workDir) throws IOException
    {
        Path real = createNfcFile(workDir);
        NfdFileSystem fs = newFileSystem(NfdFileSystem.Mode.DECODE_TO_NFC);

        Path a = fs.getPath(real.toString());
        Path b = fs.getPath(real.toString());
        assertThat(a, is(b));
        assertThat(a.hashCode(), is(b.hashCode()));
    }

    @Test
    public void testUriEntryPointsUnsupported()
    {
        NfdFileSystemProvider provider = new NfdFileSystemProvider();
        assertThrows(UnsupportedOperationException.class, () -> provider.getFileSystem(java.net.URI.create("nfd:///")));
        assertThrows(UnsupportedOperationException.class, () -> provider.getPath(java.net.URI.create("nfd:///")));
    }
}
