/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.fluid;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

class BCFluidInitializationTest {
	@Test
	void resolvesPrivateFlowingFluidWallPredicateDuringClassInitialization() throws Exception {
		Class<?> fluidClass = assertDoesNotThrow(
				() -> Class.forName(BCFluid.class.getName(), true, BCFluid.class.getClassLoader()));

		Field field = fluidClass.getDeclaredField("CAN_PASS_THROUGH_WALL");
		field.setAccessible(true);
		MethodHandle handle = (MethodHandle) field.get(null);

		assertNotNull(handle);
		assertEquals(
				MethodType.methodType(boolean.class, FlowingFluid.class, Direction.class, BlockGetter.class,
						BlockPos.class, BlockState.class, BlockPos.class, BlockState.class),
				handle.type());
	}
}
