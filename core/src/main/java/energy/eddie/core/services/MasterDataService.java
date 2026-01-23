// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import energy.eddie.api.agnostic.master.data.MasterData;
import energy.eddie.api.agnostic.master.data.MasterDataCollection;
import energy.eddie.api.agnostic.master.data.MeteredDataAdministrator;
import energy.eddie.api.agnostic.master.data.PermissionAdministrator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
public class MasterDataService implements MasterDataCollection {
    private final List<MasterData> masterDataList = new ArrayList<>();

    public void registerMasterData(MasterData masterData) {
        masterDataList.add(masterData);
    }

    @Override
    public List<PermissionAdministrator> getPermissionAdministrators() {
        return masterDataList.stream()
                             .map(MasterData::permissionAdministrators)
                             .flatMap(List::stream)
                             .toList();
    }

    @Override
    public Optional<PermissionAdministrator> getPermissionAdministrator(String id) {
        return masterDataList.stream()
                             .map(masterData -> masterData.getPermissionAdministrator(id))
                             .filter(Optional::isPresent)
                             .findAny()
                             .flatMap(Function.identity());
    }

    @Override
    public List<MeteredDataAdministrator> getMeteredDataAdministrators() {
        return masterDataList.stream()
                             .map(MasterData::meteredDataAdministrators)
                             .flatMap(List::stream)
                             .toList();
    }

    @Override
    public Optional<MeteredDataAdministrator> getMeteredDataAdministrator(String id) {
        return masterDataList.stream()
                             .map(masterData -> masterData.getMeteredDataAdministrator(id))
                             .filter(Optional::isPresent)
                             .findAny()
                             .flatMap(Function.identity());
    }

    public List<MasterData> masterData() {
        return masterDataList;
    }
}
