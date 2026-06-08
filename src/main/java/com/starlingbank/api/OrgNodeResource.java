package com.starlingbank.api;

import com.starlingbank.model.OrgNode;
import com.starlingbank.service.OrgChartService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/orgnodes")
public class OrgNodeResource {

    private final OrgChartService orgChartService;

    @Inject
    public OrgNodeResource(OrgChartService orgChartService) {
        this.orgChartService = orgChartService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<OrgNode> getOrgNodes() {
        return List.copyOf(orgChartService.getOrgNodes().values());
    }
}
