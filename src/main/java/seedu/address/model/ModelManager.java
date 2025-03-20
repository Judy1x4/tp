package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.model.cca.Attendance;
import seedu.address.model.cca.Cca;
import seedu.address.model.cca.CcaInformation;
import seedu.address.model.cca.SessionCount;
import seedu.address.model.person.Person;

/**
 * Represents the in-memory model of the address book data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final AddressBook addressBook;
    private final UserPrefs userPrefs;
    private final FilteredList<Person> filteredPersons;

    /**
     * Initializes a ModelManager with the given addressBook and userPrefs.
     */
    public ModelManager(ReadOnlyAddressBook addressBook, ReadOnlyUserPrefs userPrefs) {
        requireAllNonNull(addressBook, userPrefs);

        logger.fine("Initializing with address book: " + addressBook + " and user prefs " + userPrefs);

        this.addressBook = new AddressBook(addressBook);
        this.userPrefs = new UserPrefs(userPrefs);
        filteredPersons = new FilteredList<>(this.addressBook.getPersonList());
    }

    public ModelManager() {
        this(new AddressBook(), new UserPrefs());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public void setUserPrefs(ReadOnlyUserPrefs userPrefs) {
        requireNonNull(userPrefs);
        this.userPrefs.resetData(userPrefs);
    }

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    @Override
    public Path getAddressBookFilePath() {
        return userPrefs.getAddressBookFilePath();
    }

    @Override
    public void setAddressBookFilePath(Path addressBookFilePath) {
        requireNonNull(addressBookFilePath);
        userPrefs.setAddressBookFilePath(addressBookFilePath);
    }

    //=========== AddressBook ================================================================================

    @Override
    public void setAddressBook(ReadOnlyAddressBook addressBook) {
        this.addressBook.resetData(addressBook);
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return addressBook;
    }

    @Override
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return addressBook.hasPerson(person);
    }

    @Override
    public void deletePerson(Person target) {
        addressBook.removePerson(target);
    }

    @Override
    public void addPerson(Person person) {
        addressBook.addPerson(person);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    @Override
    public void setPerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);

        addressBook.setPerson(target, editedPerson);
    }

    @Override
    public boolean hasCca(Cca cca) {
        requireNonNull(cca);
        return addressBook.hasCca(cca);
    }

    @Override
    public void addCca(Cca cca) {
        addressBook.addCca(cca);
    }

    @Override
    public void deleteCca(Cca target) {
        requireNonNull(target);
        addressBook.removeCca(target);
        removeCcaFromAllStudents(target);
    }

    private void removeCcaFromAllStudents(Cca cca) {
        for (Person person : addressBook.getPersonList()) {
            if (person.getCcas().contains(cca)) {
                Set<CcaInformation> newCcaInformation = new HashSet<>(person.getCcaInformation());
                newCcaInformation.removeIf(c -> c.getCca().equals(cca));
                Person newPerson = new Person(person.getName(), person.getPhone(), person.getEmail(),
                        person.getAddress(), newCcaInformation);
                addressBook.setPerson(person, newPerson);
            }
        }
    }

    @Override
    public void setCca(Cca target, Cca editedCca) {
        requireAllNonNull(target, editedCca);

        addressBook.setCca(target, editedCca);
    }

    @Override
    public void recordAttendance(Cca cca, Person person, int amount) throws IllegalArgumentException {
        requireAllNonNull(person, cca, amount);
        Set<CcaInformation> ccaInformations = person.getCcaInformation();
        CcaInformation ccaInformation = ccaInformations.stream()
                .filter(c -> c.getCca().equals(cca))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Person does not have this CCA"));
        Attendance attendance = ccaInformation.getAttendance();
        Attendance newAttendance = new Attendance(new SessionCount(
                attendance.getSessionsAttended().getSessionCount() + amount), attendance.getTotalSessions());
        CcaInformation newCcaInformation = new CcaInformation(cca, ccaInformation.getRole(), newAttendance);
        Set<CcaInformation> newCcaInformations = new HashSet<>(ccaInformations);
        newCcaInformations.remove(ccaInformation);
        newCcaInformations.add(newCcaInformation);
        addressBook.setPerson(person, new Person(person.getName(), person.getPhone(), person.getEmail(),
                person.getAddress(), newCcaInformations));
    }

    //=========== Filtered Person List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Cca} backed by the internal list of
     * {@code versionedAddressBook}
     */
    @Override
    public ObservableList<Cca> getCcaList() {
        return addressBook.getCcaList();
    }

    /**
     * Returns an unmodifiable view of the list of {@code Person} backed by the internal list of
     * {@code versionedAddressBook}
     */
    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return filteredPersons;
    }

    @Override
    public void updateFilteredPersonList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        filteredPersons.setPredicate(predicate);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ModelManager)) {
            return false;
        }

        ModelManager otherModelManager = (ModelManager) other;
        return addressBook.equals(otherModelManager.addressBook)
                && userPrefs.equals(otherModelManager.userPrefs)
                && filteredPersons.equals(otherModelManager.filteredPersons);
    }

}
