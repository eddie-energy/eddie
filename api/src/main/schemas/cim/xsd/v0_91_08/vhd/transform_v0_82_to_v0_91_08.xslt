<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:old="http://www.eddie.energy/VHD/EDD01/20240614"
                xmlns:new="https://eddie.energy/CIM"
                version="1.0">

    <xsl:output method="xml" indent="yes"/>

    <!-- Root transformation -->
    <xsl:template match="/old:ValidatedHistoricalData_Envelope">
        <new:VHD_Enveloppe>
            <xsl:apply-templates select="old:MessageDocumentHeader"/>
            <xsl:apply-templates select="old:ValidatedHistoricalData_MarketDocument"/>
        </new:VHD_Enveloppe>
    </xsl:template>

    <!-- Rename MessageDocumentHeader and adjust structure -->
    <xsl:template match="old:MessageDocumentHeader">
        <new:messageDocumentHeader.creationDateTime>
            <xsl:value-of select="old:creationDateTime"/>
        </new:messageDocumentHeader.creationDateTime>
        <new:messageDocumentHeader.metaInformation.connectionId>
            <xsl:value-of select="old:MessageDocumentHeader_MetaInformation/old:connectionid"/>
        </new:messageDocumentHeader.metaInformation.connectionId>
        <new:messageDocumentHeader.metaInformation.dataNeedId>
            <xsl:value-of select="old:MessageDocumentHeader_MetaInformation/old:dataNeedid"/>
        </new:messageDocumentHeader.metaInformation.dataNeedId>
        <new:messageDocumentHeader.metaInformation.documentType>
            <xsl:value-of select="old:MessageDocumentHeader_MetaInformation/old:dataType"/>
        </new:messageDocumentHeader.metaInformation.documentType>
        <new:messageDocumentHeader.metaInformation.permissionId>
            <xsl:value-of select="old:MessageDocumentHeader_MetaInformation/old:permissionid"/>
        </new:messageDocumentHeader.metaInformation.permissionId>
        <new:messageDocumentHeader.metaInformation.region.connector>
            <xsl:value-of
                    select="old:MessageDocumentHeader_MetaInformation/old:MessageDocumentHeader_Region/old:connector"/>
        </new:messageDocumentHeader.metaInformation.region.connector>
        <new:messageDocumentHeader.metaInformation.region.country>
            <xsl:value-of
                    select="old:MessageDocumentHeader_MetaInformation/old:MessageDocumentHeader_Region/old:connector"/>
        </new:messageDocumentHeader.metaInformation.region.country>
    </xsl:template>

    <!-- Transform Market Document -->
    <xsl:template match="old:ValidatedHistoricalData_MarketDocument">
        <new:MarketDocument>
            <xsl:apply-templates select="*"/>
        </new:MarketDocument>
    </xsl:template>

    <!-- Drop old elements -->
    <xsl:template match="old:revisionNumber"/>
    <xsl:template match="old:type"/>

    <xsl:template match="old:sender_MarketParticipant.mRID">
        <new:sender_MarketParticipant.mRID>
            <xsl:attribute name="codingScheme">
                <xsl:value-of select="old:codingScheme"/>
            </xsl:attribute>
            <xsl:value-of select="old:value"/>
        </new:sender_MarketParticipant.mRID>
    </xsl:template>

    <xsl:template match="old:receiver_MarketParticipant.mRID">
        <new:receiver_MarketParticipant.mRID>
            <xsl:attribute name="codingScheme">
                <xsl:value-of select="old:codingScheme"/>
            </xsl:attribute>
            <xsl:value-of select="old:value"/>
        </new:receiver_MarketParticipant.mRID>
    </xsl:template>

    <!-- Rename TimeSeriesList structure -->
    <xsl:template match="old:TimeSeriesList">
        <new:TimeSeries>
            <xsl:apply-templates select="old:TimeSeries"/>
        </new:TimeSeries>
    </xsl:template>

    <!-- Rename TimeSeries -->
    <xsl:template match="old:TimeSeries">
        <!-- TODO: New time series elements and timeseries attributes-->
        <new:timeSeriesElement>
            <xsl:apply-templates select="*"/>
        </new:timeSeriesElement>
    </xsl:template>

    <!-- Adjust reason structure -->
    <xsl:template match="old:ReasonList">
        <new:reason>
            <xsl:apply-templates select="old:Reason"/>
        </new:reason>
    </xsl:template>

    <xsl:template match="old:Reason">
        <new:reason.code>
            <xsl:value-of select="old:code"/>
        </new:reason.code>
        <new:reason.text>Not Specified</new:reason.text>
    </xsl:template>

    <!-- Generic transformation of elements, adjusting namespaces -->
    <xsl:template match="*">
        <xsl:element name="new:{local-name()}">
            <xsl:apply-templates select="@* | node()"/>
        </xsl:element>
    </xsl:template>

    <!-- Copy attributes unchanged -->
    <xsl:template match="@*">
        <xsl:copy/>
    </xsl:template>
</xsl:stylesheet>
