package com.bloodlink.repository.memory;

import com.bloodlink.model.*;
import com.bloodlink.repository.BloodBagRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class MemoryBloodBagRepository implements BloodBagRepository {

    private final List<BloodBag> bags = new ArrayList<>();
    private final AtomicInteger ids = new AtomicInteger(1);

    public MemoryBloodBagRepository() {
        seed();
    }

    /**
     * Seeds a realistic shelf: mostly released stock, a few bags still in
     * quarantine awaiting screening, some near expiry and a couple already
     * expired so the demo shows the expiry sweep doing real work.
     */
    private void seed() {
        LocalDate today = LocalDate.now();
        Object[][] plan = {
                // group, component, count, collected days ago, released
                {BloodGroup.O_POS, BloodComponent.PRBC, 14, 6, true},
                {BloodGroup.O_POS, BloodComponent.PRBC, 5, 38, true},
                {BloodGroup.O_NEG, BloodComponent.PRBC, 6, 9, true},
                {BloodGroup.O_NEG, BloodComponent.PRBC, 2, 41, true},
                {BloodGroup.A_POS, BloodComponent.PRBC, 12, 11, true},
                {BloodGroup.A_POS, BloodComponent.PLATELETS, 4, 2, true},
                {BloodGroup.A_NEG, BloodComponent.PRBC, 5, 20, true},
                {BloodGroup.B_POS, BloodComponent.PRBC, 11, 14, true},
                {BloodGroup.B_POS, BloodComponent.PLASMA, 6, 60, true},
                {BloodGroup.B_NEG, BloodComponent.PRBC, 4, 27, true},
                {BloodGroup.AB_POS, BloodComponent.PRBC, 7, 17, true},
                {BloodGroup.AB_NEG, BloodComponent.PRBC, 3, 30, true},
                {BloodGroup.O_POS, BloodComponent.WHOLE_BLOOD, 3, 1, false},
                {BloodGroup.A_POS, BloodComponent.WHOLE_BLOOD, 2, 1, false},
                {BloodGroup.B_NEG, BloodComponent.WHOLE_BLOOD, 2, 0, false},
                {BloodGroup.AB_POS, BloodComponent.PLATELETS, 2, 0, false},
                {BloodGroup.O_POS, BloodComponent.PLATELETS, 2, 6, true},
                {BloodGroup.A_NEG, BloodComponent.PLATELETS, 1, 7, true}
        };

        String[] donorNames = {"Aarav Sharma", "Diya Mehta", "Kabir Joshi", "Ananya Rao", "Rohan Patil"};
        int donorIndex = 0;

        for (Object[] row : plan) {
            BloodGroup group = (BloodGroup) row[0];
            BloodComponent component = (BloodComponent) row[1];
            int count = (Integer) row[2];
            int daysAgo = (Integer) row[3];
            boolean released = (Boolean) row[4];

            for (int i = 0; i < count; i++) {
                LocalDate collected = today.minusDays(daysAgo);
                String name = donorNames[donorIndex % donorNames.length];
                int donorId = 1001 + (donorIndex % donorNames.length);
                donorIndex++;

                BloodBag bag = BloodBag.collected(nextCode(collected), group, component, donorId, name, collected);
                bag.setId(ids.getAndIncrement());

                if (released) {
                    for (TtiMarker m : TtiMarker.values()) bag.setResult(m, TestResult.NON_REACTIVE);
                    bag.setStatus(bag.isExpiredOn(today) ? BagStatus.EXPIRED : BagStatus.AVAILABLE);
                }
                bags.add(bag);
            }
        }

        // Issue history, so the wastage percentage on the dashboard is meaningful.
        BloodGroup[] issuedGroups = {BloodGroup.O_POS, BloodGroup.A_POS, BloodGroup.B_POS, BloodGroup.O_NEG, BloodGroup.AB_POS};
        for (int i = 0; i < 55; i++) {
            LocalDate collected = today.minusDays(50 + (i % 30));
            BloodBag issued = BloodBag.collected(nextCode(collected), issuedGroups[i % issuedGroups.length],
                    BloodComponent.PRBC, 1001 + (i % 5), donorNames[i % donorNames.length], collected);
            issued.setId(ids.getAndIncrement());
            for (TtiMarker m : TtiMarker.values()) issued.setResult(m, TestResult.NON_REACTIVE);
            issued.setStatus(BagStatus.ISSUED);
            bags.add(issued);
        }

        // Two bags that quietly passed their expiry date while the application was
        // closed. The sweep at startup is what catches these.
        for (int i = 0; i < 2; i++) {
            LocalDate collected = today.minusDays(44 + i);
            BloodBag stale = BloodBag.collected(nextCode(collected), BloodGroup.A_POS,
                    BloodComponent.PRBC, 1002, "Diya Mehta", collected);
            stale.setId(ids.getAndIncrement());
            for (TtiMarker m : TtiMarker.values()) stale.setResult(m, TestResult.NON_REACTIVE);
            stale.setStatus(BagStatus.AVAILABLE);
            bags.add(stale);
        }

        // One historical reactive unit, so the discard and deferral path is visible in the demo.
        BloodBag reactive = BloodBag.collected(nextCode(today.minusDays(21)), BloodGroup.B_POS,
                BloodComponent.WHOLE_BLOOD, 1003, "Kabir Joshi", today.minusDays(21));
        reactive.setId(ids.getAndIncrement());
        for (TtiMarker m : TtiMarker.values()) reactive.setResult(m, TestResult.NON_REACTIVE);
        reactive.setResult(TtiMarker.HBV, TestResult.REACTIVE);
        reactive.setStatus(BagStatus.DISCARDED);
        bags.add(reactive);
    }

    private final AtomicInteger codeSequence = new AtomicInteger(1000);

    private String nextCode(LocalDate collected) {
        return String.format("BL-%s-%04d",
                collected.toString().replace("-", "").substring(2),
                codeSequence.incrementAndGet());
    }

    @Override public synchronized List<BloodBag> findAll() { return new ArrayList<>(bags); }

    @Override public synchronized Optional<BloodBag> findById(int id) {
        return bags.stream().filter(b -> b.getId() == id).findFirst();
    }

    @Override public synchronized BloodBag save(BloodBag bag) {
        if (bag.getId() == 0) { bag.setId(ids.getAndIncrement()); bags.add(bag); }
        return bag;
    }

    @Override public synchronized void update(BloodBag bag) {
        for (int i = 0; i < bags.size(); i++)
            if (bags.get(i).getId() == bag.getId()) { bags.set(i, bag); return; }
    }
}
