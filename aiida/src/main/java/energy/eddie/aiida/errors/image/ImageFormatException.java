// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.image;


public class ImageFormatException extends Exception {
    public ImageFormatException(String filename) {
        super("Unsupported format for file %s".formatted(filename));
    }
}
