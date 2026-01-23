// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.image;


public class ImageReadException extends Exception {
    public ImageReadException(String filename, Exception e) {
        super("Cannot read file %s".formatted(filename), e);
    }
}
