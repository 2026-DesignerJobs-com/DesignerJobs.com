package at.ac.fhcampuswien;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// seeds jobs.json with randomly generated listings on first run
@Component
public class DummyGenerator implements CommandLineRunner {

    private final JobStorage storage;

    private static final String[] ADJECTIVES = {
        "creative", "experienced", "skilled", "innovative", "passionate",
        "modern", "clean", "minimal", "bold", "dynamic",
        "versatile", "detail-oriented"
    };

    private static final String[] ACTIONS = {
        "design", "create", "craft", "develop",
        "deliver", "build", "produce", "revamp"
    };

    private static final String[] CLIENTS = {
        "startup", "agency", "brand", "small business",
        "enterprise", "boutique studio", "non-profit", "tech company"
    };

    private static final String[] UIUX_PROJECTS = {
        "mobile app", "web dashboard", "design system", "onboarding flow",
        "user portal", "admin panel", "checkout flow", "booking interface"
    };

    private static final String[] GRAPHIC_PROJECTS = {
        "brand identity", "logo", "packaging design", "poster series",
        "social media kit", "visual identity", "infographic", "print collateral"
    };

    private static final String[] INTERIOR_SPACES = {
        "office space", "restaurant", "retail store", "co-working space",
        "hotel lobby", "apartment", "showroom", "cafe"
    };

    private static final String[] WEB_PROJECTS = {
        "portfolio site", "e-commerce store", "landing page", "corporate website",
        "blog", "product showcase", "event page", "membership site"
    };

    private static final String[] LOCATIONS = {"Vienna", "Berlin", "Munich", "Remote", "Zurich", "Amsterdam"};
    private static final String[] BUDGETS   = {"small", "medium", "big"};
    private static final String[] WORKMODES = {"remote", "on site", "hybrid"};

    public DummyGenerator(JobStorage storage) {
        this.storage = storage;
    }

    @Override
    public void run(String... args) {
        if (!storage.load().isEmpty()) return; // if not empty, assume jobs.json already seeded

        List<Job> jobs = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            jobs.add(uiuxJob());
            jobs.add(graphicJob());
            jobs.add(interiorJob());
            jobs.add(webJob());
        }
        storage.save(jobs);
    }

    // --- one generator per job type ---

    private Job uiuxJob() {
        String project = pick(UIUX_PROJECTS);
        return job(
            pick(ADJECTIVES) + " UI/UX Designer for " + project,
            sentence("ui/ux designer", project),
            "webdesign"
        );
    }

    private Job graphicJob() {
        String project = pick(GRAPHIC_PROJECTS);
        return job(
            pick(ADJECTIVES) + " Graphic Designer for " + project,
            sentence("graphic designer", project),
            "graphic design"
        );
    }

    private Job interiorJob() {
        String space = pick(INTERIOR_SPACES);
        return job(
            pick(ADJECTIVES) + " Interior Designer for " + space,
            sentence("interior designer", space),
            "interior design"
        );
    }

    private Job webJob() {
        String project = pick(WEB_PROJECTS);
        return job(
            pick(ADJECTIVES) + " Web Designer for " + project,
            sentence("web designer", project),
            "webdesign"
        );
    }

    // builds a description from word pools
    private String sentence(String role, String subject) {
        return "seeking a " + pick(ADJECTIVES) + " " + role
            + " to " + pick(ACTIONS) + " a " + subject
            + " for our " + pick(CLIENTS);
    }

    private Job job(String title, String description, String category) {
        Job j = new Job();
        j.id = UUID.randomUUID().toString();
        j.title = title;
        j.description = description;
        j.category = category;
        j.location = pick(LOCATIONS);
        j.budget = pick(BUDGETS);
        j.workMode = pick(WORKMODES);
        j.createdAt = Instant.now().toString();
        return j;
    }

    // picks a random element from an array
    private String pick(String[] arr) {
        return arr[(int) (Math.random() * arr.length)];
    }
}
