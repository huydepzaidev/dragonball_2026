package nro.models.services;

import nro.models.player.NewPet;

public final class NarutoPetPartTest {

    private NarutoPetPartTest() {
    }

    public static void main(String[] args) {
        require(PetService.NARUTO_PET_HEAD_PART == 2102);
        require(PetService.NARUTO_PET_BODY_PART == 1990);
        require(PetService.NARUTO_PET_LEG_PART == 1991);

        NewPet pet = new NewPet(null,
                PetService.NARUTO_PET_HEAD_PART,
                PetService.NARUTO_PET_BODY_PART,
                PetService.NARUTO_PET_LEG_PART);
        require(pet.getHead() >= 0);
        require(pet.getHead() == 2102);
        require(pet.getBody() == 1990);
        require(pet.getLeg() == 1991);

        System.out.println("NARUTO_PET_PART_OK head=" + pet.getHead()
                + " body=" + pet.getBody() + " leg=" + pet.getLeg());
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
