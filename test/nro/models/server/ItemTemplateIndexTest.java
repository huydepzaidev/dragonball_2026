package nro.models.server;

import java.util.ArrayList;
import java.util.List;
import nro.models.player_system.Template.ItemTemplate;

public final class ItemTemplateIndexTest {

    private ItemTemplateIndexTest() {
    }

    public static void main(String[] args) {
        List<ItemTemplate> templates = new ArrayList<>();

        ItemTemplate first = template(0, "first");
        Manager.addItemTemplateAtId(templates, first);
        require(templates.size() == 1);
        require(templates.get(0) == first);

        ItemTemplate chest = template(2041, "Rương hợp tác Naruto");
        Manager.addItemTemplateAtId(templates, chest);
        require(templates.size() == 2042);
        require(templates.get(2041) == chest);
        for (int id = 0; id < templates.size(); id++) {
            require(templates.get(id) != null);
            require(templates.get(id).id == id);
        }

        ItemTemplate replacement = template(2019, "Pet Cửu Vĩ Hồ");
        Manager.addItemTemplateAtId(templates, replacement);
        require(templates.get(2019) == replacement);
        require(templates.get(2018).id == 2018);

        System.out.println("ITEM_TEMPLATE_INDEX_OK size=" + templates.size()
                + " chest=" + templates.get(2041).name);
    }

    private static ItemTemplate template(int id, String name) {
        ItemTemplate item = new ItemTemplate();
        item.id = (short) id;
        item.name = name;
        return item;
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
