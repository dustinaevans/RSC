package com.openrsc.server.event.rsc.impl.combat.scripts.all;

import com.openrsc.server.event.rsc.impl.combat.scripts.CombatScript;
import com.openrsc.server.model.container.Equipment;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.Mob;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.util.rsc.DataConversions;

/**
 * @author Imposter
 */
public class PlayerPoisonScript implements CombatScript {

	@Override
	public void executeScript(Mob attacker, Mob victim) {
		if (victim.isPlayer()) {
			Player p = (Player) victim;
			if (p.isAntidoteProtected()) {
				return;
			}
			p.setPoisonDamage(p.getSkills().getLevel(3));
			p.startPoisonEvent();
		}
	}

	@Override
	public boolean shouldExecute(Mob attacker, Mob victim) {
		if (attacker.isPlayer() && DataConversions.random(0, 100) <= 10) {
			Player p = (Player) attacker;
			if (p.getDuel().isDuelActive()) {
				return false;
			}
			if (attacker.getWorld().getServer().getConfig().WANT_EQUIPMENT_TAB) {
				Item i;
				for (int q = 0; q < Equipment.slots; q++) {
					i = p.getEquipment().get(q);
					if (i == null)
						continue;
					if (i.getDef(attacker.getWorld()).getName().toLowerCase().contains("poisoned"))
						return true;
				}
			} else {
				for (Item i : p.getInventory().getItems()) {
					if (i.getDef(attacker.getWorld()).getName().toLowerCase().contains("poisoned")
						&& i.isWielded()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean shouldCombatStop() {
		return false;
	}

}
