package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.node.StoryNode;
import com.tuckersoft.branchengine.node.StoryNodeRepository;
import com.tuckersoft.branchengine.notification.RealityLogRepository;
import com.tuckersoft.branchengine.playthrough.Playthrough;
import com.tuckersoft.branchengine.playthrough.PlaythroughRepository;
import com.tuckersoft.branchengine.playthrough.PlaythroughStatus;
import com.tuckersoft.branchengine.security.CurrentUserService;
import com.tuckersoft.branchengine.user.Role;
import com.tuckersoft.branchengine.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Tests unitarios del DecisionService: sin PostgreSQL, sin red y sin contexto de Spring. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DecisionServiceTest {

    @Mock
    private DecisionRepository decisionRepository;
    @Mock
    private PlaythroughRepository playthroughRepository;
    @Mock
    private StoryNodeRepository storyNodeRepository;
    @Mock
    private RealityLogRepository realityLogRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DecisionService decisionService;

    private User owner;
    private StoryNode origin;
    private StoryNode bus;
    private StoryNode mirror;
    private Playthrough playthrough;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setEmail("stefan@tuckersoft.test");
        owner.setDisplayName("Stefan Butler");
        owner.setRole(Role.ROLE_USER);

        origin = node(10L, "NODE-CEREAL", "NODE-BUS", "NODE-ESPEJO");
        bus = node(11L, "NODE-BUS", null, null);
        mirror = node(12L, "NODE-ESPEJO", null, null);

        playthrough = new Playthrough();
        playthrough.setId(100L);
        playthrough.setPlayerTag("STEFAN-01");
        playthrough.setUser(owner);
        playthrough.setStartNodeCode(origin.getNodeCode());
        playthrough.setCurrentNode(origin);
        playthrough.setLucidity(100);
        playthrough.setControlLevel(0);
        playthrough.setStatus(PlaythroughStatus.ACTIVA);
        playthrough.setCreatedAt(Instant.now());
        playthrough.setUpdatedAt(Instant.now());

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(playthrough));
        when(storyNodeRepository.findByNodeCode(anyString())).thenReturn(Optional.empty());
        when(storyNodeRepository.findByNodeCode("NODE-BUS")).thenReturn(Optional.of(bus));
        when(storyNodeRepository.findByNodeCode("NODE-ESPEJO")).thenReturn(Optional.of(mirror));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(inv -> {
            Decision d = inv.getArgument(0);
            d.setId(500L);
            return d;
        });
    }

    @Test
    void laCamaraGanaALaRebeldiaPorPrecedenciaDeReglas() {
        DecisionResponse res = decide("Stefan destruye la camara", "LEVE");

        assertThat(res.branchType()).isEqualTo("RUPTURA_CUARTA_PARED");
        assertThat(res.handlerUnit()).isEqualTo("Departamento Netflix");
        assertThat(res.outcomeCode()).isEqualTo("BREAK_FOURTH_WALL");
        assertThat(res.resolvedNodeCode()).isEqualTo("NODE-ESPEJO");
    }

    @Test
    void unTextoSinLetrasEsEntradaCorruptaYNoTocaLaPartida() {
        DecisionResponse res = decide("%%%% 01001 ### @@@ 110", "CRITICO");

        assertThat(res.branchType()).isEqualTo("ENTRADA_CORRUPTA");
        assertThat(res.status()).isEqualTo("ERROR");
        assertThat(res.resolvedNodeCode()).isNull();
        assertThat(playthrough.getLucidity()).isEqualTo(100);
        assertThat(playthrough.getControlLevel()).isZero();
        assertThat(playthrough.getStatus()).isEqualTo(PlaythroughStatus.ACTIVA);
        assertThat(playthrough.getCurrentNode()).isSameAs(origin);
        verify(playthroughRepository, never()).save(any());
    }

    @Test
    void elImpactoCriticoAplicaMenos40Mas45SinPasarseDeLosLimites() {
        DecisionResponse first = decide("Stefan sigue adelante con el guion.", "CRITICO");
        assertThat(first.lucidity()).isEqualTo(60);
        assertThat(first.controlLevel()).isEqualTo(45);

        // Cerca de los limites: 20 - 40 no baja de 0 y 70 + 45 no pasa de 100
        playthrough.setStatus(PlaythroughStatus.ACTIVA);
        playthrough.setLucidity(20);
        playthrough.setControlLevel(70);
        DecisionResponse second = decide("Stefan sigue adelante con el guion.", "CRITICO");
        assertThat(second.lucidity()).isZero();
        assertThat(second.controlLevel()).isEqualTo(100);
    }

    @Test
    void controlLevel100TerminaEnPacSymbolAunqueLaLucidezTambienSea0() {
        playthrough.setLucidity(40);
        playthrough.setControlLevel(55);

        DecisionResponse res = decide("Stefan cree que lo vigilan por el televisor.", "CRITICO");

        assertThat(res.lucidity()).isZero();
        assertThat(res.controlLevel()).isEqualTo(100);
        assertThat(res.playthroughStatus()).isEqualTo("FINALIZADA");
        assertThat(res.endingCode()).isEqualTo("ENDING_PAC_SYMBOL");
        assertThat(playthrough.getCurrentNode()).isSameAs(origin);
    }

    @Test
    void publishEventSeLlamaUnaVezEnDecisionNormalYCeroEnEntradaCorrupta() {
        decide("Stefan acepta la oferta de Mohan.", "LEVE");
        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));

        reset(eventPublisher);
        decide("%%% 0101 ### @@ 11", "LEVE");
        verify(eventPublisher, never()).publishEvent(any());
    }

    private DecisionResponse decide(String rawInput, String impact) {
        return decisionService.create(new DecisionRequest(100L, rawInput, impact), null);
    }

    private static StoryNode node(Long id, String code, String primary, String glitch) {
        StoryNode n = new StoryNode();
        n.setId(id);
        n.setNodeCode(code);
        n.setTitle("Escena " + code);
        n.setSceneText("Texto de la escena " + code);
        n.setBranchCapacity(10);
        n.setCurrentBranches(0);
        n.setPrimaryBranchCode(primary);
        n.setGlitchBranchCode(glitch);
        n.setCreatedAt(Instant.now());
        return n;
    }
}
