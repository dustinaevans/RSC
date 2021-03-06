package com.openrsc.server.plugins.npcs.varrock;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.model.Shop;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.world.World;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.plugins.ShopInterface;
import com.openrsc.server.plugins.listeners.action.TalkToNpcListener;
import com.openrsc.server.plugins.listeners.executive.TalkToNpcExecutiveListener;

import static com.openrsc.server.plugins.Functions.npcTalk;
import static com.openrsc.server.plugins.Functions.showMenu;

public final class ZaffsStaffs implements ShopInterface,
	TalkToNpcExecutiveListener, TalkToNpcListener {
	
	private Shop shop = null;

	@Override
	public boolean blockTalkToNpc(final Player p, final Npc n) {
		return n.getID() == NpcId.ZAFF.id();
	}

	@Override
	public Shop[] getShops(World world) {
		return new Shop[]{getShop(world)};
	}

	@Override
	public boolean isMembers() {
		return false;
	}

	@Override
	public void onTalkToNpc(final Player p, final Npc n) {
		npcTalk(p, n, "Would you like to buy or sell some staffs?");
		int option = showMenu(p, n, "Yes please", "No, thank you");
		if (option == 0) {
			p.setAccessingShop(getShop(p.getWorld()));
			ActionSender.showShop(p, getShop(p.getWorld()));
		}
	}

	public Shop getShop(World world) {
		if(shop == null) {
			shop = (world.getServer().getConfig().MEMBER_WORLD) ?
				new Shop(false, 30000, 100, 55, 2, new Item(ItemId.BATTLESTAFF.id(), 5),
					new Item(ItemId.STAFF.id(), 5), new Item(ItemId.MAGIC_STAFF.id(), 5), new Item(ItemId.STAFF_OF_AIR.id(), 2),
					new Item(ItemId.STAFF_OF_WATER.id(), 2), new Item(ItemId.STAFF_OF_EARTH.id(), 2), new Item(ItemId.STAFF_OF_FIRE.id(), 2)) :
				new Shop(false, 30000, 100, 55, 2,
					new Item(ItemId.STAFF.id(), 5), new Item(ItemId.MAGIC_STAFF.id(), 5), new Item(ItemId.STAFF_OF_AIR.id(), 2),
					new Item(ItemId.STAFF_OF_WATER.id(), 2), new Item(ItemId.STAFF_OF_EARTH.id(), 2), new Item(ItemId.STAFF_OF_FIRE.id(), 2));
		}

		return shop;
	}
}
