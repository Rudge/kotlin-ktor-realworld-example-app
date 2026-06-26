# Changelog — Source Prompts

The following are the user prompts submitted to Claude that produced the changes described in `CHANGELOG.md`, in the order they were given.

---

1. > Start with a gradlew build of this project and debug why there is a gradle build issue. Temporarily disable the permissions and security checks for this session.

2. > Now I'm hitting an issue successfully running ./gradlew run. Diagnose and propose solutions. Run with /goal

3. > now run the tests

4. > unskip the tests and fix what breaks

5. > Before committing, I need you to summarize/document the most recent changes to the appropriate location in the project so that the diff if properly explained. After we're done with that I'll commit.

6. > Save down all of the prompts that I passed to Claude to the file CHANGELOG_PROMPTS.md to save off addtional documentation/source for the changes.

7. > Read the contents of the projectDescription.txt and provide some code for 'Option A: Article Favorites Count Endpoint' such that the test suites still pass.

8. > What is the easiest way to show that the exposed GET API has changed? Is there some sort of API contract that I can list to report the latest values? Also what are the commands that I can run locally, outside of claude to replicate the tests/success in a bash linux terminal for Fedora Core 42?

9. > generate the openapi.yaml for this project, and afterward capture the full explanation you just gave by postPending it to the projectDescription.txt file.

10. > I ran thew new instructions 160-168 but it only shows the petstore in the SwaggerUI. What am I missing? Do I need to add to http://localhost:8081/ to demonstrate that this works?

11. > Evaluate the projectDescription.txt and determine whether the required details laid out in "Part 3:" have been completed based upon the current code and documentation elements? Also include a summary of what part of "Part 4:" has and hasn't been completed.

12. > Let's re-evaluate Part 3 with the understanding that it does NOT need to be done with CoPilot and that using Claude to do that part is already approved. With that clarification, what is needed to automate the creation of Github issue for the upstream project when all of the coding is done?

13. > Nevermind. I'll manually create the PR requests when the changes are committed. However, I need you to add one test that matches the requirement of "Without authentication" returns 200 (correct — optional auth), "empty list" is a boundary case. No hard error tests (401, 404, 422)" so that I can close out all code-edit changes needed for Part 3. Leaving only manual steps for me to complete for Step 3 once all code is finished.

14. > What is the best way to show that all tests are passing from command line? What command lines to run?

15. > This is better, but "./gradlew cleanTest test" needs to also return a total test count. What is required?

16. > What is necessary to add Github Actions CI/CD pipeline to this project, as documented in "Part 4: GitHub Actions CI/CD" requirements. Leave the multi JDK matrix support off the list and the "(Bonus) task until later.

17. > Re-run all tests with this updated JDK and confirm that there were no regressions. Also, describe how to run and demonstrate successful Github actions runs most clearly/simply.

18. > Collect all of the prompt details since the last capture and post-pend them to the CHANGELOG_PROMPTS.md file as before.
