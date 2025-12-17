package energy.eddie.regionconnector.nl.mijn.aansluiting.tasks;

import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoint;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class AccountingPointFilterTask implements UnaryOperator<Tuple2<List<MijnAansluitingResponse>, List<MeteringPoint>>> {
    @Override
    public Tuple2<List<MijnAansluitingResponse>, List<MeteringPoint>> apply(Tuple2<List<MijnAansluitingResponse>, List<MeteringPoint>> tuple) {
        var meters = new ArrayList<MeteringPoint>();
        for (var response : tuple.getT1()) {
            for (var register : response.marketEvaluationPoint().registerList()) {
                for (var meteringPoint : tuple.getT2()) {
                    if (register.meter().mrid().equals(meteringPoint.getEan())) {
                        meters.add(meteringPoint);
                    }
                }
            }
        }
        return Tuples.of(tuple.getT1(), meters);
    }
}
