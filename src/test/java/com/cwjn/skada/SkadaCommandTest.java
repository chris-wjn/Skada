package com.cwjn.skada;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/** Verifies generation commands select the intended namespace or individual-entry route. */
class SkadaCommandTest {
    /** Initializes vanilla registries required by command argument types. */
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** A bare mod ID must enumerate its namespace rather than resolve it as an entity ID. */
    @Test
    void mobsMinecraftSelectsNamespaceGeneration() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        new SkadaCommand(dispatcher, mock(CommandBuildContext.class, RETURNS_DEEP_STUBS));
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.hasPermission(2)).thenReturn(true);

        var parsed = dispatcher.parse("skada generate mobs minecraft", source);
        assertEquals("namespace", parsed.getContext().getNodes().get(3).getNode().getName());
        assertEquals("minecraft", parsed.getContext().getArguments().get("namespace").getResult());
    }

    /** The explicit entity branch keeps individual IDs separate from namespace generation. */
    @Test
    void mobsEntitySelectsIndividualGeneration() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        new SkadaCommand(dispatcher, mock(CommandBuildContext.class, RETURNS_DEEP_STUBS));
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.hasPermission(2)).thenReturn(true);

        var parsed = dispatcher.parse("skada generate mobs entity minecraft:zombie", source);
        assertEquals("", parsed.getReader().getRemaining());
        assertEquals(new ResourceLocation("minecraft", "zombie"),
                parsed.getContext().getArguments().get("entity").getResult());
        assertEquals("entity", parsed.getContext().getNodes().get(3).getNode().getName());
    }

    /** The all-namespaces literal must still take precedence over namespace arguments. */
    @Test
    void mobsAllSelectsAllNamespaces() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        new SkadaCommand(dispatcher, mock(CommandBuildContext.class, RETURNS_DEEP_STUBS));
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.hasPermission(2)).thenReturn(true);

        var parsed = dispatcher.parse("skada generate mobs all", source);
        assertEquals("all", parsed.getContext().getNodes().get(3).getNode().getName());
    }
}
