// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v1_04;

import energy.eddie.cim.v1_04.vhd.Point;

import java.util.List;

record PointsWithResolution(List<Point> points, String resolution, String start, String end) {
}
