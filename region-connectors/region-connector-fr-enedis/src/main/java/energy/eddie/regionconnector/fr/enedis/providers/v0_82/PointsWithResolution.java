package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.cim.v0_82.vhd.PointComplexType;

import java.util.List;

record PointsWithResolution(List<PointComplexType> points, String resolution, String start, String end) {
}
