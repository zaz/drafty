import * as shell from "shelljs";

shell.cp("-R", "src/public/js/modules/fuse/", "dist/public/js/modules/");
shell.cp("-R", "src/public/fonts", "dist/public/");
shell.cp("-R", "src/public/webfonts", "dist/public/");
shell.cp("-R", "src/public/images", "dist/public/");
shell.cp("-R", ".env", "dist/");