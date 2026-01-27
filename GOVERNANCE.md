# Project Governance and Internal Organization

## 1. Overview

This document defines the governance structure, roles, responsibilities, and interaction model for the project within the Linux Foundation Energy ecosystem. The model ensures scientific integrity, technical excellence, business sustainability, and community-driven development in alignment with LF Energy principles.

The governance structure combines academic leadership, industrial strategy, and open-source engineering practices to establish a transparent, scalable, and sustainable project organization.

---

## 2. Mission and Scope – Internal Teams

The project unites academic institutions, industry partners, and developers to deliver open, interoperable, and production-ready software solutions for the energy domain.

The internal organization is structured into the following functional teams:

### Scientific & Governance Team

- Oversees research quality, publication strategy, and academic partnerships.
- Monthly meetings chaired by the _Administrative and Scientific Coordinator_

### Technical Architecture Team

- Defines system architecture and ensures technical integrity.
- Monthly meetings chaired by the _Technical Coordinator_

### Development & Engineering Team

- Implements features, maintains quality, and manages releases.
- Bi-weekly meetings chaired by the _Development Coordinator_

### Business & Ecosystem Team

- Drives community growth, dissemination, and stakeholder engagement.
- Bi-weekly meetings chaired by the _Administrative and Scientific Coordinator_

### Framework Teams

Dedicated teams for core components:

- EDDIE Framework Team
- AIIDA Framework Team

### General Assembly

The _General Assembly_ consists of appointed individuals of 16 consortial members of the EDDIE Project.

- The initial distribution of roles is set via Project EDDIE consortium.
- Meetings every 12 months (online) chaired by the _Administrative and Scientific Coordinator_
- Extraordinary meetings may be initiated by the _Administrative and Scientific Coordinator_, by the _Business Coordinator_ or the _Technical Coordinator_ three weeks in advance.
- Agenda, location and key performance indicators must be sent at least 5 business days before the meeting
- Main decision-making body, and in particular _General Assembly_ may
    - assignments of the roles stated
    - addition of new _General Assembly_ members,
    - removal or exchange of _General Assembly_ members
- The _General Assembly_ decides on strategic decisions on the project
- All GA decisions require a 50% majority

Minutes of the meetings mentioned above to be stored in the project GitHub.

---

## 3. Governance Roles and Nominations

### Administrative and Scientific Coordinator

**Nominee:** FH-Prof. Priv.-Doz. Dr. Oliver Hödl (University of Vienna)  
**Focus:** Scientific leadership and project governance.

**Responsibilities:**

- Academic and scientific direction
- Strategic partnerships
- Community growth
- Project representation
- Institutional coordination
- Oversight of publications and dissemination
- Strategic reporting

---

### Business Coordinator

**Nominee:** Laurent Schmitt (Digital4Grids)  
**Focus:** Stakeholder management and ecosystem development.

**Responsibilities:**

- Industry engagement
- Use case alignment
- Sustainability planning
- Quality assurance and compliance
- Joint leadership of community building

---

### Technical Coordinator

**Nominee:** Georg Hartner (Entarc.eu GmbH)  
**Focus:** Architecture and system design.

**Responsibilities:**

- Architecture governance
- Technical alignment
- Interoperability strategy
- Documentation
- Conflict resolution
- Joint leadership of community building
- Architecture coordination with development leadership

---

### Development Coordinator

**Nominee:** FH-Prof. Dr. Marc Kurz (FH Oberösterreich)  
**Focus:** Development leadership and quality assurance.

**Responsibilities:**

- Development processes
- Coding guidelines
- QA and testing
- Release management
- CI/CD governance
- Architecture implementation alignment

---

### Framework Leads

#### EDDIE Framework Lead

**Nominee:** Florian Weingartshofer (FH Oberösterreich)  
**Focus:** Ownership and roadmap of EDDIE.

#### AIIDA Lead

**Nominee:** Stefan Penzinger (FH Oberösterreich)  
**Focus:** Ownership and roadmap of AIIDA.

---

## 4. Organizational Diagram

```
                         Administrative & Scientific Coordinator
                        FH-Prof. Priv.-Doz. Dr. Oliver Hödl
                                     │
                                     │
       ┌─────────────────────────────┼─────────────────────────────┐─────────────────────────────┐
       │                             │                             │                             │
 Business Coordinator         Technical Coordinator          Development Coordinator    Operationalisation Lead
  Laurent Schmitt              Georg Hartner              FH-Prof. Dr. Marc Kurz          Nathaniel Boisgard
 (Digital4Grids)           (Entarc.eu GmbH)               (FH Oberösterreich)            (University of Vienna)
       │                             │                             │
       │──────────── Community Building (joint responsibility) ────────────│
       │                             │                             │
       │                             ├──── Architecture Alignment ────┐
       │                             │                                 │
       │                             │                                 │
       │                      Architecture &                     Development &
       │                      Integration Team                   Engineering Team
       │                                                             │
       │                                                             │
       │                                     ┌───────────────────────┴──────────────────────┐
       │                                     │                                              │
       │                            EDDIE Framework Lead                               AIIDA Lead
       │                           Florian Weingartshofer                          Stefan Penzinger
       │                                 (FH OÖ)                                       (FH OÖ)
```

## 5. Governance Charter

This project operates according to LF Energy principles of openness, collaboration, meritocracy, and transparency.

The governance model is designed to ensure long-term sustainability through a balanced structure that integrates scientific excellence, technical leadership, business stewardship, and community engagement.

Strategic oversight is provided by the **Administrative and Scientific Coordinator**, ensuring research quality, scientific integrity, and external academic alignment.  
Technical leadership and architectural coherence are led by the **Technical Coordinator** in close cooperation with the **Development Coordinator**, who is responsible for implementation standards, software quality, and operational excellence.

The **Business Coordinator** leads ecosystem development, industrial engagement, and strategic partnerships, working jointly with the **Technical Coordinator** on community building and representation within the LF Energy ecosystem.

Ownership of core software components is assigned to Framework Leads, who ensure roadmap continuity, quality control, and contributor coordination.

The project adheres to the LF Energy principles of vendor neutrality, open contribution, and technical transparency.

---

## 6. Responsibility Matrix (RACI)

| Activity                 | Admin & Scientific | Business | Technical | Development | EDDIE Lead | AIIDA Lead | Operationalisation Lead |
|--------------------------|--------------------|----------|-----------|-------------|------------|------------|-------------------------| 
| Scientific leadership    | R                  | I        | C         | C           | I          | I          | I                       |
| Academic coordination    | R                  | I        | C         | C           | I          | I          | I                       |
| Business strategy        | C                  | R        | C         | I           | I          | I          | C                       |
| Partner engagement       | I                  | R        | C         | I           | I          | I          | C                       |
| Community building       | R                  | C        | C         | I           | C          | C          | C                       |
| Architecture design      | I                  | I        | R         | C           | C          | C          | C                       |
| Development leadership   | I                  | I        | C         | R           | C          | C          | C                       |
| Coding standards         | I                  | I        | C         | R           | C          | C          | C                       |
| Quality assurance        | I                  | I        | C         | R           | C          | C          | C                       |
| Release management       | I                  | I        | C         | C           | C          | C          | R                       |
| Documentation            | C                  | C        | C         | R           | C          | C          | I                       |
| EDDIE roadmap            | I                  | I        | C         | R           | C          | I          | C                       |
| AIIDA roadmap            | I                  | I        | C         | R           | C          | C          | C                       |
| LF Energy representation | I                  | R        | R         | I           | I          | I          | I                       |

**Legend:**  
R = Responsible  
C = Consulted  
I = Informed

---

## 7. Roles and Interactions – One-Pager

This section describes how roles interact in practice to ensure governance efficiency and technical coherence.

---

### Community Building

**Joint responsibility of:**

- Business Coordinator
- Technical Coordinator

**Purpose:**

- Foster contributor growth.
- Align community activities with technical direction.
- Promote adoption and visibility.
- Manage LF Energy engagement and external communication.

---

### Architecture Alignment

**Joint responsibility of:**

- Technical Coordinator
- Development Coordinator

**Purpose:**

- Translate architecture into engineering standards.
- Ensure consistency across frameworks.
- Guide design decisions.
- Reduce technical debt.
- Maintain scalability.

---

### Scientific and Industrial Balance

**Joint responsibility of:**

- Administrative & Scientific Coordinator
- Business Coordinator

**Purpose:**

- Maintain academic excellence.
- Align research with industry needs.
- Promote knowledge transfer.
- Support sustainability.

---

### Framework Ownership

**Owned by:**

- EDDIE Framework Lead
- AIIDA Lead

**Purpose:**

- Ensure roadmap continuity.
- Maintain code quality.
- Guide contributor activity.
- Act as first technical contact per framework.

---

## 8. Operating Principles

The project is governed by the following principles:

- Open governance
- Transparent decision-making
- Merit-based contribution
- Technical excellence
- Scientific integrity
- Community-driven development
- Vendor neutrality
- Interoperability focus
- Sustainability by design

---

## 9. Summary

This governance structure ensures:

- Clear accountability
- Scalable leadership
- Quality assurance
- Ecosystem growth
- Architecture coherence
- Industrial relevance
- Scientific excellence

The model supports sustainable growth while remaining aligned with LF Energy governance values and open-source best practices.
