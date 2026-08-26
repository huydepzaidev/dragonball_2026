package nro.models.services;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.network.MySession;
import nro.models.player.Player;
import nro.models.player_system.Template;

public final class InventoryBagPacketTest {

    private InventoryBagPacketTest() {
    }

    public static void main(String[] args) throws Exception {
        try (ServerSocket listener = new ServerSocket(0);
                Socket client = new Socket("127.0.0.1", listener.getLocalPort());
                Socket server = listener.accept()) {
            CapturingSession session = new CapturingSession(server);
            Player player = new Player();
            player.setSession(session);
            player.inventory.itemsBag.clear();
            player.inventory.itemsBag.add(item((short) 45));
            player.inventory.itemsBag.add(new Item());
            player.inventory.itemsBag.add(item((short) 556));
            player.inventory.itemsBag.add(new Item());

            InventoryService.gI().sendItemBags(player);

            check(session.data != null, "Bag packet was not sent");
            assertClientCanReadEveryDeclaredSlot(session.data);
        }
        System.out.println("INVENTORY_BAG_PACKET_TEST_OK");
    }

    private static void assertClientCanReadEveryDeclaredSlot(byte[] data) throws Exception {
        try (DataInputStream reader = new DataInputStream(new ByteArrayInputStream(data))) {
            check(reader.readByte() == 0, "Unexpected bag packet action");
            int slotCount = reader.readUnsignedByte();
            check(slotCount == 4, "Unexpected bag slot count");
            for (int slot = 0; slot < slotCount; slot++) {
                short itemId = reader.readShort();
                if (itemId == -1) {
                    continue;
                }
                reader.readInt();
                reader.readUTF();
                reader.readUTF();
                int optionCount = reader.readUnsignedByte();
                for (int option = 0; option < optionCount; option++) {
                    reader.readUnsignedByte();
                    reader.readUnsignedShort();
                }
            }
            check(reader.available() == 0, "Bag packet contains unread bytes");
        }
    }

    private static Item item(short id) {
        Item item = new Item();
        item.template = new Template.ItemTemplate(
                id, (byte) 2, (byte) 1, "Test item", "", (short) 0, (short) -1,
                false, 0);
        item.quantity = 1;
        return item;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class CapturingSession extends MySession {

        private byte[] data;

        private CapturingSession(Socket socket) {
            super(socket);
        }

        @Override
        public void sendMessage(Message message) {
            data = message.getData();
        }
    }
}
