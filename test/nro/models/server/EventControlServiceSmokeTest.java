package nro.models.server;

import java.sql.Connection;
import java.lang.reflect.Method;
import java.util.Set;
import nro.models.data.LocalManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

public final class EventControlServiceSmokeTest {

    private EventControlServiceSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        JSONArray first = new JSONArray();
        first.add(691L);
        first.add(2L);
        first.add("[]");
        first.add(123L);
        JSONArray second = new JSONArray();
        second.add(5L);
        second.add(1L);
        second.add("[]");
        second.add(456L);
        JSONArray inventory = new JSONArray();
        inventory.add(first.toJSONString());
        inventory.add(second.toJSONString());

        EventControlService.FilterResult filtered = EventControlService.filterInventoryJson(
                inventory.toJSONString(), Set.of(691));
        if (filtered.removed() != 1) {
            throw new AssertionError("Exactly one summer item must be removed");
        }
        JSONArray result = (JSONArray) JSONValue.parse(filtered.value());
        JSONArray emptySlot = (JSONArray) JSONValue.parse((String) result.get(0));
        JSONArray keptSlot = (JSONArray) JSONValue.parse((String) result.get(1));
        if (((Number) emptySlot.get(0)).intValue() != -1
                || ((Number) keptSlot.get(0)).intValue() != 5
                || ((Number) emptySlot.get(3)).longValue() != 123L) {
            throw new AssertionError("Inventory JSON structure was not preserved");
        }

        JSONArray pet = new JSONArray();
        pet.add("[]");
        pet.add("[]");
        pet.add(inventory.toJSONString());
        pet.add("[]");
        EventControlService.FilterResult filteredPet = EventControlService.filterPetJson(
                pet.toJSONString(), Set.of(691));
        if (filteredPet.removed() != 1) {
            throw new AssertionError("Pet equipment must be filtered");
        }

        try (Connection con = LocalManager.getConnection()) {
            EventControlService.gI().load(con);
        }
        Method loadPurgeItemIds = EventControlService.class
                .getDeclaredMethod("loadPurgeItemIds", String.class);
        loadPurgeItemIds.setAccessible(true);
        Set<Integer> euroPurgeItems = (Set<Integer>) loadPurgeItemIds.invoke(
                EventControlService.gI(), "euro");
        if (!EventControlService.gI().isAvailable()
                || !EventControlService.gI().isEnabled(EventControlService.SUMMER)
                || EventControlService.gI().isEnabled(EventControlService.CHRISTMAS)
                || !EventControlService.gI().canAcquireItem(691)
                || EventControlService.gI().canAcquireItem(648)
                || !EventControlService.gI().canAcquireItem(1628)
                || euroPurgeItems.contains(1628)
                || EventControlService.gI().isEnabled(EventControlService.HUNG_VUONG)
                || EventControlService.gI().canCreateBoss(-355)
                || EventControlService.gI().canCreateBoss(-353)
                || EventControlService.gI().canOpenNpc(51, 21)) {
            throw new AssertionError("Database event gates do not match the seeded state");
        }

        System.out.println("EVENT_CONTROL_SERVICE_SMOKE_TEST_OK");
    }
}
