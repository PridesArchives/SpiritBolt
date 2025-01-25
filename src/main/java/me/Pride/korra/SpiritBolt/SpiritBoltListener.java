package me.Pride.korra.SpiritBolt;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.PlayerSwingEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class SpiritBoltListener implements Listener {

	@EventHandler
	public void onSneak(PlayerToggleSneakEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (event.isSneaking()) {
			Player player = event.getPlayer();
			BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

			if (bPlayer == null) {
				return;
			}
			CoreAbility ability = bPlayer.getBoundAbility();

			if (ability == null) {
				return;
			}
			if (bPlayer.canBendIgnoreCooldowns(ability)) {
				if (ability instanceof AvatarAbility && bPlayer.isElementToggled(Element.AVATAR)) {
					if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SpiritBolt")) {
						new SpiritBolt(player);
					}
				}
			}
		}
	}

	@EventHandler
	public void onSwing(PlayerSwingEvent event) {
		if (event.isCancelled()) {
			return;
		}
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

		if (bPlayer == null) {
			return;
		}
		CoreAbility ability = bPlayer.getBoundAbility();

		if (ability == null) {
			return;
		}
		if (bPlayer.canBendIgnoreCooldowns(ability)) {
			if (ability instanceof AvatarAbility && bPlayer.isElementToggled(Element.AVATAR)) {
				if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SpiritBolt")) {
					if (CoreAbility.hasAbility(player, SpiritBolt.class)) {
						if (SpiritBolt.isCharged(player)) {
							SpiritBolt.shootBolt(player);
						}
					}
				}
			}
		}
	}
}
